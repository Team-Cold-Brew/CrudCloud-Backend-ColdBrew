package com.riwi.CrudCloud.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.common.models.Database;
import com.riwi.CrudCloud.common.models.DbType;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, Long> {

    /**
     * Consulta para contar las bases de datos activas de un usuario (Plan Individual).
     * Se considera activa si no tiene marca de borrado lógico (deletedAt IS NULL) y su estado no es DELETED.
     * @param userId ID del usuario.
     * @return Número de bases de datos activas.
     */
    @Query("SELECT COUNT(d) FROM Database d WHERE d.user.userId = :userId AND d.deletedAt IS NULL AND d.status != 'DELETED'")
    Long countActiveByUserId(Long userId);

    /**
     * Consulta para contar las bases de datos activas de una organización (Plan Organizacional).
     * Se considera activa si no tiene marca de borrado lógico (deletedAt IS NULL) y su estado no es DELETED.
     * @param organizationId ID de la organización.
     * @return Número de bases de datos activas.
     */
    @Query("SELECT COUNT(d) FROM Database d WHERE d.organization.organizationId = :organizationId AND d.deletedAt IS NULL AND d.status != 'DELETED'")
    Long countActiveByOrganizationId(Long organizationId);

    /**
     * Lista todas las bases de datos no borradas lógicamente, creadas por un usuario específico.
     * @param userId ID del usuario.
     * @return Lista de bases de datos.
     */
    @Query("SELECT d FROM Database d WHERE d.user.userId = :userId AND d.deletedAt IS NULL")
    List<Database> findByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * Busca una base de datos por el puerto asignado y que no haya sido borrada lógicamente.
     * Nota: En el nuevo modelo, múltiples DBs compartirán puerto y host. Este método podría volverse obsoleto
     * o usarse para asegurar que los puertos de los CONTENEDORES COMPARTIDOS no se dupliquen si tuvieras que crearlos.
     * @param port Puerto a verificar.
     * @return Optional que contiene la base de datos si el puerto está en uso.
     */
    Optional<Database> findByPortAndDeletedAtIsNull(Integer port);

    /**
     * Nuevo método necesario para la lógica de contenedores compartidos.
     * Cuenta cuántas bases de datos están alojadas en un contenedor específico, excluyendo las borradas.
     * @param containerId ID del contenedor compartido.
     * @return Número de bases de datos alojadas.
     */
    @Query("SELECT COUNT(d) FROM Database d WHERE d.containerId = :containerId AND d.deletedAt IS NULL AND d.status != 'DELETED'")
    Long countActiveByContainerId(String containerId);

    /**
     * Nuevo método para obtener un listado de todos los IDs de contenedores compartidos en uso
     * para un tipo de motor específico (ej. todos los IDs de contenedores MySQL).
     * Esto nos ayuda a encontrar un contenedor disponible o saber cuáles están en uso.
     * @param dbType Tipo de motor (MySQL, PostgreSQL, etc.).
     * @return Lista de IDs de contenedores compartidos.
     */
    @Query("SELECT DISTINCT d.containerId FROM Database d WHERE d.dbType = :dbType AND d.deletedAt IS NULL")
    List<String> findDistinctContainerIdsByDbType(DbType dbType);
}