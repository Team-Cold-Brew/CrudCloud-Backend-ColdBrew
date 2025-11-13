package com.riwi.CrudCloud.instance.model;

import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import com.riwi.CrudCloud.instance.model.ENUM.InstanceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "instance")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long instanceId;

    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private InstanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private DbType dbType;

    @Column(length = 100, nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(length = 50, nullable = false)
    private String username;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 255)
    private String containerId;

    @Column(nullable = false)
    private Boolean pdfDownloadStatus = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}