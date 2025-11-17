package com.riwi.CrudCloud.database.service;

import com.riwi.CrudCloud.common.models.DatabaseStatus;
import com.riwi.CrudCloud.common.models.DbType;
import com.riwi.CrudCloud.common.models.Database;
import com.riwi.CrudCloud.common.models.Plan;
import com.riwi.CrudCloud.common.models.User;
import com.riwi.CrudCloud.database.dto.DatabaseResponse;
import com.riwi.CrudCloud.database.dto.DatabaseCreateRequest;
import com.riwi.CrudCloud.database.config.SharedContainerConfig;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.CustomBadRequestException;
import com.riwi.CrudCloud.common.util.exception.classes.client_errors.CustomNotFoundException;
import com.riwi.CrudCloud.database.repository.DatabaseRepository;
import com.riwi.CrudCloud.database.repository.PlanRepositoryByInstance;
import com.riwi.CrudCloud.database.repository.UserRepositoryByIntance;
import com.riwi.CrudCloud.database.util.EncryptionUtil;
import com.riwi.CrudCloud.database.util.MailService;
import com.riwi.CrudCloud.database.util.PdfGeneratorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final DatabaseManagementService dbManagementService; // Usa hosts internos
    private final SharedContainerConfig sharedContainerConfig;
    private final MailService mailService;
    private final PdfGeneratorService pdfGeneratorService;

    @Value("${crudcloud.public.host:localhost}")
    private String publicHostAddress;

    private static final int MAX_DBS_PER_CONTAINER = 1000;


    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    private void validateInstanceLimit(Long userId, Long organizationId, Plan plan) {
        Long currentCount = (organizationId != null)
                ? databaseRepository.countActiveByOrganizationId(organizationId)
                : databaseRepository.countActiveByUserId(userId);

        if (currentCount >= plan.getMaxDatabases()) {
            throw new CustomBadRequestException("Database limit reached. Your plan (" + plan.getName() + ") allows up to " + plan.getMaxDatabases() + " databases.");
        }
    }

    private String generateDatabaseName(DbType dbType, String userDefinedName, String planName) {
        String cleanName = (userDefinedName != null) ? userDefinedName.replaceAll("[^a-zA-Z0-9_]", "") : "";

        if (planName.equalsIgnoreCase("Free") || cleanName.isEmpty()) {
            return dbType.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        }
        return "db_" + cleanName + "_" + UUID.randomUUID().toString().substring(0, 4);
    }

    private String generateUniqueUsername(DbType dbType) {
        return "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * Select an available shared container that is not full.
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


    /**
     * Create a new logical database within a shared container.
     */
    @Transactional
    public DatabaseResponse createDatabase(DatabaseCreateRequest request) {

        User user = userRepository.findById(Long.valueOf(request.getUserId()))
                .orElseThrow(() -> new CustomNotFoundException("User not found."));

        Integer planId = user.getPersonalPlan() != null ? user.getPersonalPlan().getPlanId() : null;

        if (planId == null) {
            throw new CustomNotFoundException("User does not have an assigned plan.");
        }

        Plan plan = planRepository.findById(Math.toIntExact(planId))
                .orElseThrow(() -> new CustomNotFoundException("Plan not found for the user."));

        validateInstanceLimit(Long.valueOf(request.getUserId()), request.getOrganizationId(), plan);

        // Select Container (obtains information with internal host)
        SharedContainerConfig.ContainerInfo assignedContainer = allocateContainer(request.getDbType());

        int externalPort = assignedContainer.port();
        if (request.getDbType() == DbType.POSTGRESQL && assignedContainer.port() == 5432) {
            externalPort = 5434;
        }

        // Generate Credentials and Names
        String rawPassword = generateRandomPassword();
        String encryptedPassword = EncryptionUtil.encrypt(rawPassword);
        String dbName = generateDatabaseName(request.getDbType(), request.getName(), plan.getName());
        String username = generateUniqueUsername(request.getDbType());

        // Provision in the Real Engine (Use the INTERNAL HOST for administration)
        dbManagementService.createDatabaseInContainer(
                assignedContainer.containerId(),
                request.getDbType(),
                dbName,
                username,
                rawPassword
        );

        // Save to Central Database (Use PUBLIC HOST for user)
        Database newDatabase = Database.builder()
                .name(dbName)
                .user(user)
                .organization(request.getOrganizationId() != null ? null : null)
                .status(DatabaseStatus.RUNNING)
                .dbType(request.getDbType())
                .host(publicHostAddress)
                .port(externalPort)
                .username(username)
                .password(encryptedPassword)
                .containerId(assignedContainer.containerId())
                .pdfDownloadStatus(false)
                .build();

        newDatabase = databaseRepository.save(newDatabase);

        mailService.sendInstanceCreationEmail(newDatabase, username, publicHostAddress, externalPort);

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
     * Suspend access to the database (Logical + Revoke permissions).
     */
    @Transactional
    public DatabaseResponse suspendDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        if (database.getStatus() == DatabaseStatus.SUSPENDED) {
            throw new CustomBadRequestException("Database is already suspended.");
        }

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
     * Restore access.
     */
    @Transactional
    public DatabaseResponse resumeDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

        if (database.getStatus() == DatabaseStatus.RUNNING) {
            throw new CustomBadRequestException("Database is already running.");
        }

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
     * Delete the database (Drop + Soft Delete).
     */
    @Transactional
    public void deleteDatabase(Long databaseId) {
        Database database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new CustomNotFoundException("Database not found."));

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

        dbManagementService.rotatePassword(
                database.getContainerId(),
                database.getDbType(),
                database.getUsername(),
                newRawPassword
        );

        database.setPassword(newEncryptedPassword);
        database.setPdfDownloadStatus(false);
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

    /**
     * Map the Database entity to its response DTO.
     */
    private DatabaseResponse mapToResponse(Database db) {
        return DatabaseResponse.builder()
                .databaseId(db.getDatabaseId())
                .name(db.getName())
                .userId(Long.valueOf(db.getUser().getUserId()))
                .status(db.getStatus())
                .dbType(db.getDbType())
                .host(db.getHost())
                .port(db.getPort())
                .username(db.getUsername())
                .createdAt(db.getCreatedAt())
                .build();
    }
}