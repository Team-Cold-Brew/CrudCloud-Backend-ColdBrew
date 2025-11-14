package com.riwi.CrudCloud.database.dto;

import com.riwi.CrudCloud.common.models.DatabaseStatus;
import com.riwi.CrudCloud.common.models.DbType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de una base de datos creada o consultada.
 */
@Data
@Builder
public class DatabaseResponse {
    private Long databaseId;
    private String name;
    private Integer userId;
    private Long organizationId;
    private DatabaseStatus status;
    private DbType dbType;
    private String host;
    private Integer port;
    private String username;
    private LocalDateTime createdAt;
}