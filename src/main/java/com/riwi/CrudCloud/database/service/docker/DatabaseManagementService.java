package com.riwi.CrudCloud.database.service.docker;

import com.riwi.CrudCloud.common.models.DbType;
import com.riwi.CrudCloud.database.config.SharedContainerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Servicio responsable de ejecutar comandos DDL y DCL (Create, Grant, Revoke, Drop)
 * dentro de los contenedores compartidos.
 * Reemplaza al antiguo DockerService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseManagementService {

    private final SharedContainerConfig containerConfig;

    /**
     * Crea una base de datos y un usuario con permisos dentro del contenedor compartido.
     */
    public void createDatabaseInContainer(String containerId, DbType dbType, String dbName, String username, String password) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);

        log.info("Creating database {} and user {} in container {}", dbName, username, containerId);

        try {
            switch (dbType) {
                case MYSQL -> createMysqlResources(info, dbName, username, password);
                case POSTGRESQL -> createPostgresResources(info, dbName, username, password);
                // TODO: Implement logic for MONGODB, REDIS, etc.
                default -> throw new UnsupportedOperationException("Database type not fully implemented yet: " + dbType);
            }
        } catch (Exception e) {
            log.error("Failed to provision database resources: {}", e.getMessage());
            throw new RuntimeException("Error provisioning database in shared container.", e);
        }
    }

    /**
     * Suspende el acceso a la base de datos (Revoca permisos o bloquea usuario).
     */
    public void suspendDatabaseAccess(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Suspending access for user {} in database {}", username, dbName);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' ACCOUNT LOCK;");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH NOLOGIN;");
                default -> log.warn("Suspend not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error suspending database access.", e);
        }
    }

    /**
     * Restaura el acceso a la base de datos.
     */
    public void resumeDatabaseAccess(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Resuming access for user {} in database {}", username, dbName);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' ACCOUNT UNLOCK;");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH LOGIN;");
                default -> log.warn("Resume not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error resuming database access.", e);
        }
    }

    /**
     * Elimina la base de datos y el usuario del contenedor.
     */
    public void deleteDatabaseResources(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Deleting database {} and user {}", dbName, username);

        try {
            switch (dbType) {
                case MYSQL -> {
                    executeSql(info, "DROP DATABASE IF EXISTS " + dbName + ";");
                    executeSql(info, "DROP USER IF EXISTS '" + username + "'@'%';");
                }
                case POSTGRESQL -> {
                    // En Postgres no se puede borrar una DB si hay conexiones activas, hay que matarlas primero
                    String killConns = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "';";
                    executeSql(info, killConns); // Ojo: esto requiere ejecutar en base de datos 'postgres'
                    executeSql(info, "DROP DATABASE IF EXISTS " + dbName + ";");
                    executeSql(info, "DROP USER IF EXISTS " + username + ";");
                }
                default -> log.warn("Delete not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting database resources.", e);
        }
    }

    /**
     * Rota la contraseña del usuario en el motor.
     */
    public void rotatePassword(String containerId, DbType dbType, String username, String newPassword) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Rotating password for user {}", username);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' IDENTIFIED BY '" + newPassword + "';");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH PASSWORD '" + newPassword + "';");
                default -> log.warn("Rotate password not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error rotating password.", e);
        }
    }

    // --- PRIVATE HELPER METHODS FOR SQL ---

    private void createMysqlResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) throws Exception {
        // 1. Create DB
        executeSql(info, "CREATE DATABASE `" + dbName + "`;");
        // 2. Create User
        executeSql(info, "CREATE USER '" + username + "'@'%' IDENTIFIED BY '" + password + "';");
        // 3. Grant Privileges
        executeSql(info, "GRANT ALL PRIVILEGES ON `" + dbName + "`.* TO '" + username + "'@'%';");
        executeSql(info, "FLUSH PRIVILEGES;");
    }

    private void createPostgresResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) throws Exception {
        // 1. Create User (Role)
        executeSql(info, "CREATE USER " + username + " WITH PASSWORD '" + password + "';");
        // 2. Create DB (Owner is the new user)
        executeSql(info, "CREATE DATABASE " + dbName + " OWNER " + username + ";");
    }

    private void executeSql(SharedContainerConfig.ContainerInfo info, String sql) throws Exception {
        String jdbcUrl = "";
        String driverClass = "";

        // Construir URL JDBC según motor (para conexión administrativa)
        if (info.port() == 3306) { // MySQL identification logic (simple)
            jdbcUrl = "jdbc:mysql://" + info.host() + ":" + info.port() + "/?allowPublicKeyRetrieval=true&useSSL=false";
            driverClass = "com.mysql.cj.jdbc.Driver";
        } else if (info.port() == 5432) { // Postgres
            jdbcUrl = "jdbc:postgresql://" + info.host() + ":" + info.port() + "/postgres"; // Connect to default 'postgres' db
            driverClass = "org.postgresql.Driver";
        }

        Class.forName(driverClass);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, info.adminUser(), info.adminPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}