package com.riwi.CrudCloud.common.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Nota: Las clases User y Organization se asumen en common.model también.
// Si las tienes en otra ruta, ajusta los imports.

/**
 * Entidad que representa una instancia de base de datos de un usuario/organización.
 * Ahora, esta entidad representa una BASE DE DATOS dentro de un CONTENEDOR COMPARTIDO.
 */
@Entity
@Table(name = "database") // Nombre de la tabla en el schema
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Database {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long databaseId; // Renombrado de instanceId

    @Column(length = 100, nullable = false)
    private String name; // Nombre de la base de datos (schema)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Usuario que creó la base de datos

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization; // Organización dueña (si aplica)

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private DatabaseStatus status; // Estado lógico de la DB de usuario

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private DbType dbType; // Tipo de motor de la base de datos

    @Column(length = 100, nullable = false)
    private String host; // Host del contenedor compartido (será fijo para el motor)

    @Column(nullable = false)
    private Integer port; // Puerto del contenedor compartido (será fijo para el motor)

    @Column(length = 50, nullable = false)
    private String username; // Usuario dedicado a esta DB (para la conexión del usuario final)

    @Column(length = 255, nullable = false)
    private String password; // Contraseña cifrada para el usuario dedicado

    @Column(length = 255)
    private String containerId; // ID del contenedor COMPARTIDO que aloja esta DB

    @Column(nullable = false)
    private Boolean pdfDownloadStatus = false; // Estado de descarga del PDF

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}