package com.riwi.CrudCloud.common.util.exception.classes.server_errors;

/**
 * Excepción personalizada para errores que ocurren durante la administración
 * o provisión de recursos de bases de datos (ej. fallos al crear una DB,
 * problemas de conexión JDBC, etc.) dentro del DatabaseManagementService.
 * Extiende RuntimeException para permitir el rollback transaccional automático en Spring.
 */
public class DatabaseManagementException extends RuntimeException {

    /**
     * Constructor que acepta un mensaje.
     * @param message Mensaje descriptivo del error.
     */
    public DatabaseManagementException(String message) {
        super(message);
    }

    /**
     * Constructor que acepta un mensaje y la causa original (otra excepción).
     * @param message Mensaje descriptivo del error.
     * @param cause La causa original de la excepción (ej. SQLException, MongoException).
     */
    public DatabaseManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}