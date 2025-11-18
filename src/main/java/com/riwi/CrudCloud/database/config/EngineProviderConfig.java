package com.riwi.CrudCloud.database.config;

import java.util.Arrays;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.riwi.CrudCloud.common.models.DbType;

/**
 * Configuration for available database engines by provider
 * Reads from application.properties: crudcloud.available-engines
 */
@Component
public class EngineProviderConfig {

    @Value("${crudcloud.available-engines:MySQL,PostgreSQL,MongoDB,Redis,Cassandra,SQLServer}")
    private String availableEngines;

    /**
     * Check if an engine is configured and available
     *
     * @param engine the database engine type
     * @return true if the engine is configured, false otherwise
     */
    public boolean isEngineAvailable(DbType engine) {
        if (engine == null) {
            return false;
        }
        
        Set<String> configuredEngines = getConfiguredEngines();
        return configuredEngines.contains(engine.name());
    }

    /**
     * Get set of configured engine names
     *
     * @return set of available engine names (normalized to uppercase and trimmed)
     */
    public Set<String> getConfiguredEngines() {
        return Arrays.stream(availableEngines.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get raw configuration string
     *
     * @return comma-separated list of available engines
     */
    public String getAvailableEngines() {
        return availableEngines;
    }
}
