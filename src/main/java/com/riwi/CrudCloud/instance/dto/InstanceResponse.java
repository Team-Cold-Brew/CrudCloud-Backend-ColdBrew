package com.riwi.CrudCloud.instance.dto;

import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import com.riwi.CrudCloud.instance.model.ENUM.InstanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InstanceResponse {
    private Long instanceId;
    private String name;
    private Long userId;
    private Long organizationId;
    private InstanceStatus status;
    private DbType dbType;
    private String host;
    private Integer port;
    private String username;
    private LocalDateTime createdAt;
}