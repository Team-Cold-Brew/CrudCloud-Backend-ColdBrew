package com.riwi.CrudCloud.instance.dto;


import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InstanceCreateRequest {

    @Size(min = 3, max = 100, message = "The name must be between 3 and 100 characters long.")
    private String name;

    @NotNull(message = "The database type is required.")
    private DbType dbType;

    @NotNull(message = "The creator user ID is required.")
    private Long userId;

    private Long organizationId;
}