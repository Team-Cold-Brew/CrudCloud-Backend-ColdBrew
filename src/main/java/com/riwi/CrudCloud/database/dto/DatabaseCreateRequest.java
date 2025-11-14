package com.riwi.CrudCloud.database.dto;

import com.riwi.CrudCloud.common.models.DbType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para la solicitud de creación de una nueva base de datos.
 */
@Data
public class DatabaseCreateRequest { // Renombrado de InstanceCreateRequest

    @Size(min = 3, max = 100, message = "The name must be between 3 and 100 characters long.")
    private String name;

    @NotNull(message = "The database type is required.")
    private DbType dbType;

    @NotNull(message = "The creator user ID is required.")
    private Integer userId;

    private Long organizationId;
}