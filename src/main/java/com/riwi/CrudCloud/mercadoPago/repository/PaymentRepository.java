package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.common.models.Payment;
import com.riwi.CrudCloud.common.models.PaymentStatus;

/**
 * Repository for Payment entity
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by MercadoPago payment ID
     *
     * @param mercadopagoPaymentId the MercadoPago payment ID
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByMercadopagoPaymentId(String mercadopagoPaymentId);

    /**
     * Find payment by preference ID
     *
     * @param preferenceId the preference ID
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByPreferenceId(String preferenceId);

    /**
     * Find all payments by user ID
     *
     * @param userId the user ID
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.user.userId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Integer userId);

    /**
     * Find all payments by user ID and status
     *
     * @param userId the user ID
     * @param status the payment status
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.user.userId = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") PaymentStatus status);

    /**
     * Find all payments by plan ID
     *
     * @param planId the plan ID
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.plan.planId = :planId ORDER BY p.createdAt DESC")
    List<Payment> findByPlanId(@Param("planId") Integer planId);

    /**
     * Find all approved payments by user ID
     *
     * @param userId the user ID
     * @return List of approved payments
     */
    @Query("SELECT p FROM Payment p WHERE p.user.userId = :userId AND p.status = 'APPROVED' ORDER BY p.approvedAt DESC")
    List<Payment> findApprovedPaymentsByUserId(@Param("userId") Integer userId);

    /**
     * Check if payment exists by MercadoPago payment ID
     *
     * @param mercadopagoPaymentId the MercadoPago payment ID
     * @return true if payment exists
     */
    boolean existsByMercadopagoPaymentId(String mercadopagoPaymentId);

    /**
     * Find payment by external reference
     *
     * @param externalReference the external reference
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByExternalReference(String externalReference);
}
