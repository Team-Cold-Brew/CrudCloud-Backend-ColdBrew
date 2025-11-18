package com.riwi.CrudCloud.database.service;

import com.riwi.CrudCloud.common.models.DbType;
import com.riwi.CrudCloud.database.config.SharedContainerConfig;
import com.riwi.CrudCloud.common.util.exception.classes.server_errors.DatabaseManagementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;


/**
 * Service responsible for executing DDL and DCL commands (Create, Grant, Revoke, Drop)
 * within the shared containers. Replaces the old DockerService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseManagementService {

    private final SharedContainerConfig containerConfig;


    public void createDatabaseInContainer(String containerId, DbType dbType, String dbName, String username, String password) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Creating database {} and user {} in container {}", dbName, username, containerId);

        try {
            switch (dbType) {
                case MYSQL -> createMysqlResources(info, dbName, username, password);
                case POSTGRESQL -> createPostgresResources(info, dbName, username, password);
                case SQLSERVER -> createSqlServerResources(info, dbName, username, password);
                case MONGODB -> createMongodbResources(info, dbName, username, password);
                case CASSANDRA -> createCassandraResources(info, dbName, username, password);
                case REDIS -> createRedisResources(info, password);
                default -> throw new UnsupportedOperationException("Database type not fully implemented yet: " + dbType);
            }
        } catch (Exception e) {
            log.error("Failed to provision database resources for {}: {}", dbType, e.getMessage(), e);
            throw new DatabaseManagementException("Error provisioning database in shared container: " + e.getMessage(), e);
        }
    }

    public void suspendDatabaseAccess(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Suspending access for user {} in database {}", username, dbName);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' ACCOUNT LOCK;");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH NOLOGIN;");
                case SQLSERVER -> executeSql(info, "ALTER LOGIN " + username + " DISABLE;");
                case MONGODB -> revokeMongodbAccess(info, username);
                case CASSANDRA -> revokeCassandraAccess(info, username);
                case REDIS -> rotateRedisPassword(info, "SUSPENDED_TOKEN_" + username);
                default -> log.warn("Suspend not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new DatabaseManagementException("Error suspending database access: " + e.getMessage(), e);
        }
    }

    public void resumeDatabaseAccess(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Resuming access for user {} in database {}", username, dbName);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' ACCOUNT UNLOCK;");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH LOGIN;");
                case SQLSERVER -> executeSql(info, "ALTER LOGIN " + username + " ENABLE;");
                case MONGODB -> resumeMongodbAccess(info, username);
                case CASSANDRA -> resumeCassandraAccess(info, username);
                case REDIS -> {}
                default -> log.warn("Resume not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new DatabaseManagementException("Error resuming database access: " + e.getMessage(), e);
        }
    }

    public void deleteDatabaseResources(String containerId, DbType dbType, String dbName, String username) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Deleting database {} and user {}", dbName, username);

        try {
            switch (dbType) {
                case MYSQL -> {
                    executeSql(info, "DROP DATABASE IF EXISTS `" + dbName + "`;");
                    executeSql(info, "DROP USER IF EXISTS '" + username + "'@'%';");
                }
                case POSTGRESQL -> {
                    String killConns = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + dbName + "';";
                    executeSql(info, killConns);
                    executeSql(info, "DROP DATABASE IF EXISTS " + dbName + ";");
                    executeSql(info, "DROP USER IF EXISTS " + username + ";");
                }
                case SQLSERVER -> {
                    executeSql(info, "DROP DATABASE IF EXISTS " + dbName + ";");
                    executeSql(info, "DROP LOGIN IF EXISTS " + username + ";");
                }
                case MONGODB -> deleteMongodbResources(info, dbName, username);
                case CASSANDRA -> deleteCassandraResources(info, dbName, username);
                case REDIS -> deleteRedisResources(info);
                default -> log.warn("Delete not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new DatabaseManagementException("Error deleting database resources: " + e.getMessage(), e);
        }
    }

    public void rotatePassword(String containerId, DbType dbType, String username, String newPassword) {
        SharedContainerConfig.ContainerInfo info = containerConfig.getContainerById(containerId, dbType);
        log.info("Rotating password for user {}", username);

        try {
            switch (dbType) {
                case MYSQL -> executeSql(info, "ALTER USER '" + username + "'@'%' IDENTIFIED BY '" + newPassword + "';");
                case POSTGRESQL -> executeSql(info, "ALTER USER " + username + " WITH PASSWORD '" + newPassword + "';");
                case SQLSERVER -> executeSql(info, "ALTER LOGIN " + username + " WITH PASSWORD = '" + newPassword + "' OLD_PASSWORD = 'ignored';");
                case MONGODB -> rotateMongodbPassword(info, username, newPassword);
                case CASSANDRA -> rotateCassandraPassword(info, username, newPassword);
                case REDIS -> rotateRedisPassword(info, newPassword);
                default -> log.warn("Rotate password not implemented for {}", dbType);
            }
        } catch (Exception e) {
            throw new DatabaseManagementException("Error rotating password: " + e.getMessage(), e);
        }
    }


    private void createMysqlResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) throws Exception {
        executeSql(info, "CREATE DATABASE `" + dbName + "`;");
        executeSql(info, "CREATE USER '" + username + "'@'%' IDENTIFIED BY '" + password + "';");
        executeSql(info, "GRANT ALL PRIVILEGES ON `" + dbName + "`.* TO '" + username + "'@'%';");
        executeSql(info, "FLUSH PRIVILEGES;");
    }

    private void createPostgresResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) throws Exception {
        executeSql(info, "CREATE USER " + username + " WITH PASSWORD '" + password + "';");
        executeSql(info, "CREATE DATABASE " + dbName + " OWNER " + username + ";");
    }

    private void createSqlServerResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) throws Exception {
        executeSql(info, "CREATE DATABASE " + dbName + ";");
        executeSql(info, "CREATE LOGIN " + username + " WITH PASSWORD = '" + password + "', CHECK_POLICY = OFF;");
        executeSql(info, "USE " + dbName + "; CREATE USER " + username + " FOR LOGIN " + username + ";");
        executeSql(info, "USE " + dbName + "; EXEC sp_addrolemember 'db_owner', '" + username + "';");
    }

    private void executeSql(SharedContainerConfig.ContainerInfo info, String sql) throws Exception {
        String jdbcUrl = "";
        String driverClass = "";

        if (info.port() == 3306) {
            jdbcUrl = "jdbc:mysql://" + info.host() + ":" + info.port() + "/?allowPublicKeyRetrieval=true&useSSL=false";
            driverClass = "com.mysql.cj.jdbc.Driver";
        } else if (info.port() == 5432) {
            jdbcUrl = "jdbc:postgresql://" + info.host() + ":" + info.port() + "/crudcloud_main_db";
            driverClass = "org.postgresql.Driver";
        } else if (info.port() == 1433) {
            jdbcUrl = "jdbc:sqlserver://" + info.host() + ":" + info.port() + ";databaseName=master;encrypt=false";
            driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }

        Class.forName(driverClass);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, info.adminUser(), info.adminPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }


    private MongoClient getMongoClient(SharedContainerConfig.ContainerInfo info) {
        String connectionString = String.format("mongodb://%s:%s@%s:%d/?authSource=admin", info.adminUser(), info.adminPassword(), info.host(), info.port());
        return MongoClients.create(connectionString);
    }

    private void createMongodbResources(SharedContainerConfig.ContainerInfo info, String dbName, String username, String password) {
        try (MongoClient mongoClient = getMongoClient(info)) {
            MongoDatabase userDb = mongoClient.getDatabase(dbName);

            userDb.runCommand(
                    new Document("createUser", username)
                            .append("pwd", password)
                            .append("roles", List.of(new Document("role", "dbOwner").append("db", dbName)))
            );
        }
    }

    private void revokeMongodbAccess(SharedContainerConfig.ContainerInfo info, String username) {
        try (MongoClient mongoClient = getMongoClient(info)) {
            MongoDatabase adminDb = mongoClient.getDatabase("admin");
            adminDb.runCommand(
                    new Document("updateUser", username)
                            .append("customData", new Document("status", "SUSPENDED"))
                            .append("roles", List.of())
            );
        }
    }

    private void resumeMongodbAccess(SharedContainerConfig.ContainerInfo info, String username) {

        try (MongoClient mongoClient = getMongoClient(info)) {
            MongoDatabase adminDb = mongoClient.getDatabase("admin");
            adminDb.runCommand(
                    new Document("updateUser", username)
                            .append("customData", new Document("status", "RUNNING"))
                            .append("roles", List.of(new Document("role", "readWriteAnyDatabase"), new Document("role", "dbOwner")))
            );
        }
    }

    private void deleteMongodbResources(SharedContainerConfig.ContainerInfo info, String dbName, String username) {
        try (MongoClient mongoClient = getMongoClient(info)) {
            mongoClient.getDatabase(dbName).drop();

            MongoDatabase userDb = mongoClient.getDatabase(dbName);
            userDb.runCommand(new Document("dropUser", username));
        }
    }

    private void rotateMongodbPassword(SharedContainerConfig.ContainerInfo info, String username, String newPassword) {
        try (MongoClient mongoClient = getMongoClient(info)) {
            MongoDatabase adminDb = mongoClient.getDatabase("admin");
            adminDb.runCommand(
                    new Document("updateUser", username)
                            .append("pwd", newPassword)
            );
        }
    }



    private CqlSession getCqlSession(SharedContainerConfig.ContainerInfo info) {
        DriverConfigLoader loader = DriverConfigLoader.programmaticBuilder()
                .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10))
                .build();

        return CqlSession.builder()
                .withConfigLoader(loader)
                .addContactPoint(new InetSocketAddress(info.host(), info.port()))
                .withLocalDatacenter("datacenter1")
                .withAuthCredentials(info.adminUser(), info.adminPassword())
                .build();
    }

    private void createCassandraResources(SharedContainerConfig.ContainerInfo info, String keyspaceName, String username, String password) {
        try (CqlSession session = getCqlSession(info)) {
            session.execute(String.format("CREATE ROLE IF NOT EXISTS %s WITH PASSWORD '%s' AND LOGIN = true;", username, password));

            session.execute(String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};", keyspaceName));

            session.execute(String.format("GRANT ALL PERMISSIONS ON KEYSPACE %s TO %s;", keyspaceName, username));
        }
    }

    private void revokeCassandraAccess(SharedContainerConfig.ContainerInfo info, String username) {
        try (CqlSession session = getCqlSession(info)) {
            session.execute(String.format("ALTER ROLE %s WITH LOGIN = false;", username));
        }
    }

    private void resumeCassandraAccess(SharedContainerConfig.ContainerInfo info, String username) {
        try (CqlSession session = getCqlSession(info)) {
            session.execute(String.format("ALTER ROLE %s WITH LOGIN = true;", username));
        }
    }

    private void deleteCassandraResources(SharedContainerConfig.ContainerInfo info, String keyspaceName, String username) {
        try (CqlSession session = getCqlSession(info)) {
            session.execute(String.format("DROP KEYSPACE IF EXISTS %s;", keyspaceName));

            session.execute(String.format("DROP ROLE IF EXISTS %s;", username));
        }
    }

    private void rotateCassandraPassword(SharedContainerConfig.ContainerInfo info, String username, String newPassword) {
        try (CqlSession session = getCqlSession(info)) {
            session.execute(String.format("ALTER ROLE %s WITH PASSWORD = '%s';", username, newPassword));
        }
    }



    private Jedis getJedisClient(SharedContainerConfig.ContainerInfo info) {
        Jedis jedis = new Jedis(info.host(), info.port());
        if (info.adminPassword() != null && !info.adminPassword().isEmpty()) {
            jedis.auth(info.adminPassword());
        }
        return jedis;
    }

    private void createRedisResources(SharedContainerConfig.ContainerInfo info, String dbName) {
        try (Jedis jedis = getJedisClient(info)) {

            // If the customer uses Redis 7+, we can create a user with ACL:
            // String aclCommand = String.format("ACL SETUSER %s on ~* +@all >%s", dbName, password);
            // jedis.aclSetUser(dbName, "on", "~*", "+@all", ">" + password);

            log.info("Redis provisioning complete. User will use the generated password as the AUTH token.");
        }
    }

    private void deleteRedisResources(SharedContainerConfig.ContainerInfo info) {
        try (Jedis jedis = getJedisClient(info)) {
            log.warn("Redis deletion is ONLY a logical soft delete as resources are shared at the token level.");
        }
    }

    private void rotateRedisPassword(SharedContainerConfig.ContainerInfo info, String newPassword) {
        try (Jedis jedis = getJedisClient(info)) {
            // We rotate the end user's AUTH password (if we are using ACLs).
            // jedis.aclSetUser(username, ">" + newPassword);
            log.warn("Redis password rotation requires ACLs (ACL SETUSER). The new password is the new AUTH token.");
        }
    }
}