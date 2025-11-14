package com.riwi.CrudCloud.database.service;

import com.riwi.CrudCloud.common.models.DatabaseStatus;
import com.riwi.CrudCloud.common.models.DbType;
import com.riwi.CrudCloud.common.models.Database;
import com.riwi.CrudCloud.common.models.Plan;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.database.dto.DatabaseResponse;
import com.riwi.CrudCloud.database.dto.DatabaseCreateRequest;
import com.riwi.CrudCloud.database.config.SharedContainerConfig;
import com.riwi.CrudCloud.database.exception.CustomBadRequestException;
import com.riwi.CrudCloud.database.exception.CustomNotFoundException;
import com.riwi.CrudCloud.database.repository.DatabaseRepository;
import com.riwi.CrudCloud.database.repository.PlanRepositoryByInstance;
import com.riwi.CrudCloud.database.repository.UserRepositoryByIntance;
import com.riwi.CrudCloud.database.service.docker.DatabaseManagementService;
import com.riwi.CrudCloud.database.util.EncryptionUtil;
import com.riwi.CrudCloud.database.util.MailService;
import com.riwi.CrudCloud.database.util.PdfGeneratorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseService {

    private final DatabaseRepository databaseRepository;
    private final UserRepositoryByIntance userRepository;
    private final PlanRepositoryByInstance planRepository;
    private final DatabaseManagementService dbManagementService; // Nuevo servicio
    private final SharedContainerConfig sharedContainerConfig;   // Configuración de contenedores
    private final MailService mailService;
    private final PdfGeneratorService pdfGeneratorService;

    private static final int MAX_DBS_PER_CONTAINER = 1000;

    // --- HELPER METHODS ---

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    private void validateInstanceLimit(Long userId, Long organizationId, Plan plan) {
        Long currentCount = (organizationId != null)
                ? databaseRepository.countActiveByOrganizationId(organizationId)
                : databaseRepository.countActiveByUserId(userId);

        if (currentCount >= plan.getMaxDatabases()) { // Asumiendo que Plan tiene getMaxDatabases()
            throw new CustomBadRequestException("Database limit reached. Your plan (" + plan.getName() + ") allows up to " + plan.getMaxDatabases() + " databases.");
        }
    }

    private String generateDatabaseName(DbType dbType, String userDefinedName, String planName) {
        String cleanName = (userDefinedName != null) ? userDefinedName.replaceAll("[^a-zA-Z0-9_]", "") : "";

        // En plan Free o si no hay nombre, generamos uno aleatorio
        if (planName.equalsIgnoreCase("Free") || cleanName.isEmpty()) {
            return dbType.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
        // En planes pagos, intentamos usar el nombre (añadimos prefijo para evitar colisiones globales si se desea)
        return "db_" + cleanName + "_" + UUID.randomUUID().toString().substring(0, 4);
    }

    private String generateUniqueUsername(DbType dbType) {
        // Generar un usuario único para evitar colisiones en el contenedor compartido
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Selecciona un contenedor compartido disponible que no esté lleno.
     */
    private SharedContainerConfig.ContainerInfo allocateContainer(DbType dbType) {
        List<SharedContainerConfig.ContainerInfo> availableContainers = sharedContainerConfig.getContainersForType(dbType);

        for (SharedContainerConfig.ContainerInfo container : availableContainers) {
            Long currentLoad = databaseRepository.countActiveByContainerId(container.containerId());
            if (currentLoad < MAX_DBS_PER_CONTAINER) {
                return container;
            }
        }
        throw new RuntimeException("No available infrastructure (containers) for " + dbType + ". Please contact support.");
    }

    // --- MAIN BUSINESS LOGIC ---

    /**
     * Crea una nueva base de datos lógica dentro de un contenedor compartido.
     */
    @Transactional
    public DatabaseResponse createDatabase(DatabaseCreateRequest request) { // Renombrar DTOs si es posible

        User user = userRepository.findById(Long.valueOf(request.getUserId()))
                .orElseThrow(() -> new CustomNotFoundException("User not found."));

        Integer planId = user.getPersonalPlan() != null ? user.getPersonalPlan().getPlanId() : null;

        if (planId == null) {
            throw new CustomNotFoundException("User does not have an assigned plan.");
        }

        Plan plan = planRepository.findById(Math.toIntExact(planId)) // Asumiendo lógica de plan personal
                .orElseThrow(() -> new CustomNotFoundException("Plan not found for the user."));

        validateInstanceLimit(Long.valueOf(request.getUserId()), request.getOrganizationId(), plan);

        // 1. Seleccionar Contenedor
        SharedContainerConfig.ContainerInfo assignedContainer = allocateContainer(request.getDbType());

        // 2. Generar Credenciales y Nombres
        String rawPassword = generateRandomPassword();
        String encryptedPassword = EncryptionUtil.encrypt(rawPassword);
        String dbName = generateDatabaseName(request.getDbType(), request.getName(), plan.getName());
        String username = generateUniqueUsername(request.getDbType());

        // 3. Provisionar en el Motor Real (Ejecutar SQL)
        dbManagementService.createDatabaseInContainer(
                assignedContainer.containerId(),
                request.getDbType(),
                dbName,
                username,
                rawPassword
        );

        // 4. Guardar en Base de Datos Central
        Database newDatabase = Database.builder()
                .name(dbName)
                .user(user)
                .organization(request.getOrganizationId() != null ? null : null) // Ajustar lógica org
                .status(DatabaseStatus.RUNNING)
                .dbType(request.getDbType())
                .host(assignedContainer.host())
                .port(assignedContainer.port())
                .username(username)
                .password(encryptedPassword)
                .containerId(assignedContainer.containerId())
                .pdfDownloadStatus(false)
                .build();

        newDatabase = databaseRepository.save(newDatabase);

        // 5. Notificar
        mailService.sendInstanceCreationEmail(newDatabase, username, assignedContainer.host(), assignedContainer.port());

        return mapToResponse(newDatabase);
    }

    @Transactional
    public byte[] downloadCredentialsPdf(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        if (database.getPdfDownloadStatus()) {
            throw new CustomBadRequestException("Credentials already downloaded.");
        }

        String rawPassword = EncryptionUtil.decrypt(database.getPassword());
        byte[] pdfBytes = pdfGeneratorService.generateInstanceCredentialsPdf(database, rawPassword);

        database.setPdfDownloadStatus(true);
        database.setUpdatedAt(LocalDateTime.now());
        databaseRepository.save(database);

        return pdfBytes;
    }

    /**
     * Suspende el acceso a la base de datos (Lógico + Revocar permisos).
     */
    @Transactional
    public DatabaseResponse suspendDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        if (database.getStatus() == DatabaseStatus.SUSPENDED) {
            throw new CustomBadRequestException("Database is already suspended.");
        }

        // Revocar permisos en el motor real
        dbManagementService.suspendDatabaseAccess(
                database.getContainerId(),
                database.getDbType(),
                database.getName(),
                database.getUsername()
        );

        database.setStatus(DatabaseStatus.SUSPENDED);
        database.setUpdatedAt(LocalDateTime.now());
        databaseRepository.save(database);

        return mapToResponse(database);
    }

    /**
     * Restaura el acceso.
     */
    @Transactional
    public DatabaseResponse resumeDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        if (database.getStatus() == DatabaseStatus.RUNNING) {
            throw new CustomBadRequestException("Database is already running.");
        }

        // Restaurar permisos
        dbManagementService.resumeDatabaseAccess(
                database.getContainerId(),
                database.getDbType(),
                database.getName(),
                database.getUsername()
        );

        database.setStatus(DatabaseStatus.RUNNING);
        database.setUpdatedAt(LocalDateTime.now());
        databaseRepository.save(database);

        return mapToResponse(database);
    }

    /**
     * Elimina la base de datos (Drop + Soft Delete).
     */
    @Transactional
    public void deleteDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        // Eliminar del motor real
        dbManagementService.deleteDatabaseResources(
                database.getContainerId(),
                database.getDbType(),
                database.getName(),
                database.getUsername()
        );

        database.setStatus(DatabaseStatus.DELETED);
        database.setDeletedAt(LocalDateTime.now());
        database.setUpdatedAt(LocalDateTime.now());
        databaseRepository.save(database);
    }

    @Transactional
    public DatabaseResponse rotatePassword(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        String newRawPassword = generateRandomPassword();
        String newEncryptedPassword = EncryptionUtil.encrypt(newRawPassword);

        // Aplicar cambio en el motor real
        dbManagementService.rotatePassword(
                database.getContainerId(),
                database.getDbType(),
                database.getUsername(),
                newRawPassword
        );

        database.setPassword(newEncryptedPassword);
        database.setPdfDownloadStatus(false); // Permitir descarga de nuevo
        database.setUpdatedAt(LocalDateTime.now());
        databaseRepository.save(database);

        pdfGeneratorService.generateInstanceCredentialsPdf(database, newRawPassword);
        mailService.sendPasswordRotationEmail(database, newRawPassword);

        return mapToResponse(database);
    }

    public List<DatabaseResponse> getAllUserDatabases(Long userId) {
        return databaseRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DatabaseResponse getDatabaseDetails(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));
        return mapToResponse(database);
    }

    private DatabaseResponse mapToResponse(Database db) {
        return DatabaseResponse.builder()
                .databaseId(db.getDatabaseId())
                .name(db.getName())
                .userId(db.getUser().getUserId())
                .status(db.getStatus()) // Error de tipo aquí? database.getStatus es DatabaseStatus, DTO espera InstanceStatus
                .dbType(db.getDbType())
                .host(db.getHost())
                .port(db.getPort())
                .username(db.getUsername())
                .createdAt(db.getCreatedAt())
                .build();
    }
}