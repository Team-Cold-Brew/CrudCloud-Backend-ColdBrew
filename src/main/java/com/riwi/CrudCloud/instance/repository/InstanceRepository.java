package com.riwi.CrudCloud.instance.repository;

import com.riwi.CrudCloud.instance.model.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<Instance, Long> {

    // Method for counting active instances of a user (Individual Plan)
    @Query("SELECT COUNT(i) FROM Instance i WHERE i.user.userId = :userId AND i.deletedAt IS NULL AND i.status != 'DELETED'")
    Long countActiveByUserId(Long userId);

    // Method for counting active instances of an organization (Organizational Plan)
    @Query("SELECT COUNT(i) FROM Instance i WHERE i.organization.organizationId = :organizationId AND i.deletedAt IS NULL AND i.status != 'DELETED'")
    Long countActiveByOrganizationId(Long organizationId);

    // List instances by user (for your dashboard)
    List<Instance> findByUserIdAndDeletedAtIsNull(Long userId);

    // Method to verify if the port is in use by an undeleted instance
    Optional<Instance> findByPortAndDeletedAtIsNull(Integer port);
}