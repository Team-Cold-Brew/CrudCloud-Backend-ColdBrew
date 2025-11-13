package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.mercadoPago.model.Transaction;
import com.riwi.CrudCloud.mercadoPago.model.TransactionStatus;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Find transaction by provider transaction ID (MercadoPago ID)
     */
    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);

    /**
     * Find all transactions by user ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByUserId(@Param("userId") Integer userId);

    /**
     * Find all transactions by user ID and status
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.userId = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") TransactionStatus status);

    /**
     * Find all transactions by organization ID
     */
    @Query("SELECT t FROM Transaction t WHERE t.organizationId = :organizationId ORDER BY t.createdAt DESC")
    List<Transaction> findByOrganizationId(@Param("organizationId") Integer organizationId);

    /**
     * Find all transactions by organization ID and status
     */
    @Query("SELECT t FROM Transaction t WHERE t.organizationId = :organizationId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByOrganizationIdAndStatus(@Param("organizationId") Integer organizationId, @Param("status") TransactionStatus status);

    /**
     * Find all approved transactions by user ID
     */
    List<Transaction> findByUserUserIdAndStatusOrderByApprovalDateDesc(Integer userId, TransactionStatus status);

    /**
     * Check if transaction exists by provider transaction ID
     */
    boolean existsByProviderTransactionId(String providerTransactionId);

    /**
     * Count transactions by user ID and status
     */
    Long countByUserUserIdAndStatus(Integer userId, TransactionStatus status);

    /**
     * Find pending transactions (we'll handle the time filter in the service)
     */
    List<Transaction> findByStatusOrderByCreatedAtAsc(TransactionStatus status);
}
