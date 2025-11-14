package com.riwi.CrudCloud.database.config;

import com.riwi.CrudCloud.common.models.DbType;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Configuración para gestionar los contenedores compartidos pre-existentes.
 * En un entorno real, esto podría venir de una base de datos o variables de entorno.
 * Aquí simulamos los contenedores que creaste manualmente en la VPS.
 */
@Component
@Getter
public class SharedContainerConfig {

    // Estructura para guardar la info de conexión administrativa del contenedor compartido
    public record ContainerInfo(String containerId, String host, int port, String adminUser, String adminPassword) {}

    // Mapa estático de contenedores disponibles por tipo de motor
    private final Map<DbType, List<ContainerInfo>> sharedContainers = Map.of(
            DbType.MYSQL, List.of(
                    new ContainerInfo("mysql-shared-01", "88.99.189.55", 3306, "root", "root_password_segura")
            ),
            DbType.POSTGRESQL, List.of(
                    new ContainerInfo("postgres-shared-01", "88.99.189.55", 5432, "postgres", "postgres_password_segura")
            ),
            // Ejemplo para MongoDB
            DbType.MONGODB, List.of(
                    new ContainerInfo("mongo-shared-01", "88.99.189.55", 27017, "admin", "mongo_password_segura")
            )
            // ... Agregar el resto de motores aquí
    );

    /**
     * Obtiene la lista de contenedores configurados para un motor específico.
     */
    public List<ContainerInfo> getContainersForType(DbType dbType) {
        return sharedContainers.getOrDefault(dbType, List.of());
    }

    /**
     * Busca la info de conexión de un contenedor por su ID.
     */
    public ContainerInfo getContainerById(String containerId, DbType dbType) {
        return getContainersForType(dbType).stream()
                .filter(c -> c.containerId().equals(containerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Container configuration not found for ID: " + containerId));
    }
}