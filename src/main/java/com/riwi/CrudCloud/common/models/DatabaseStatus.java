package com.riwi.CrudCloud.common.models;

/**
 * Enumeración para el estado lógico de una base de datos de usuario.
 * Estos estados ahora son lógicos (accesibilidad del usuario), no el estado del contenedor Docker.
 */
public enum DatabaseStatus {
    CREATING,
    RUNNING,
    SUSPENDED,
    DELETED
}