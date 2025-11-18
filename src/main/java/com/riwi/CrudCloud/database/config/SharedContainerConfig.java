package com.riwi.CrudCloud.database.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.riwi.CrudCloud.common.models.DbType;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for managing pre-existing shared containers.
 * * Reads the configuration from application.properties (prefix shared.container).
 * * Contains the INTERNAL HOST (Docker Network Name) and administrator credentials.
 */
@ConfigurationProperties("shared.container")
@Getter
@Setter
public class SharedContainerConfig {

    // Structure for storing administrative connection information for the shared container
    // Note: The ‘host’ here will be the INTERNAL Docker host (e.g., ‘mysql-pool’).
    public record ContainerInfo(String containerId, String host, int port, String adminUser, String adminPassword) {}

    private Map<String, List<ContainerInfo>> sharedContainers;

    /**
     * Gets the list of containers configured for a specific engine.
     *      * Converts the map's String key to DbType for searching.
     */
    public List<ContainerInfo> getContainersForType(DbType dbType) {
        String key = dbType.name().toLowerCase();
        return sharedContainers.getOrDefault(key, List.of());
    }

    /**
     * Search for the connection information of a container by its ID.
     */
    public ContainerInfo getContainerById(String containerId, DbType dbType) {
        return getContainersForType(dbType).stream()
                .filter(c -> c.containerId().equals(containerId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Container configuration not found for ID: " + containerId + " for DB Type: " + dbType));
    }
}