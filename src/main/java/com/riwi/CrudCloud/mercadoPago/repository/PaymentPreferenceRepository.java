package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.mercadoPago.model.PaymentPreference;

/**
 * Repository for PaymentPreference entity
 */
@Repository
public interface PaymentPreferenceRepository extends JpaRepository<PaymentPreference, Long> {

    /**
     * Find payment preference by MercadoPago preference ID
     *
     * @param mercadopagoPreferenceId the MercadoPago preference ID
     * @return Optional containing the preference if found
     */
    Optional<PaymentPreference> findByMercadopagoPreferenceId(String mercadopagoPreferenceId);

    /**
     * Find all preferences by user ID
     *
     * @param userId the user ID
     * @return List of preferences
     */
    @Query("SELECT pp FROM PaymentPreference pp WHERE pp.userId = :userId ORDER BY pp.createdAt DESC")
    List<PaymentPreference> findByUserId(@Param("userId") Integer userId);

    /**
     * Find all preferences by plan ID
     *
     * @param planId the plan ID
     * @return List of preferences
     */
    @Query("SELECT pp FROM PaymentPreference pp WHERE pp.planId = :planId ORDER BY pp.createdAt DESC")
    List<PaymentPreference> findByPlanId(@Param("planId") Integer planId);

    /**
     * Find preference by external reference
     *
     * @param externalReference the external reference
     * @return Optional containing the preference if found
     */
    Optional<PaymentPreference> findByExternalReference(String externalReference);

    /**
     * Find all non-expired preferences by user ID
     *
     * @param userId the user ID
     * @return List of non-expired preferences
     */
    @Query("SELECT pp FROM PaymentPreference pp WHERE pp.userId = :userId AND pp.isExpired = false ORDER BY pp.createdAt DESC")
    List<PaymentPreference> findActivePreferencesByUserId(@Param("userId") Integer userId);

    /**
     * Check if preference exists by MercadoPago preference ID
     *
     * @param mercadopagoPreferenceId the MercadoPago preference ID
     * @return true if preference exists
     */
    boolean existsByMercadopagoPreferenceId(String mercadopagoPreferenceId);
}
