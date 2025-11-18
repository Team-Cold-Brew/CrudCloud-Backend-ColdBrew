package com.riwi.CrudCloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * JPA Configuration for CrudCloud
 * Scans entities from all modules and configures repositories
 */
@Configuration
@EntityScan(basePackages = {
    "com.riwi.CrudCloud.common.models",
    "com.riwi.CrudCloud.mercadoPago.model"
})
@EnableJpaRepositories(basePackages = {
    "com.riwi.CrudCloud.auth.repository",
    "com.riwi.CrudCloud.database.repository",
    "com.riwi.CrudCloud.mercadoPago.repository"
})
public class JpaConfig {
    // Configuration for JPA and Hibernate
}
