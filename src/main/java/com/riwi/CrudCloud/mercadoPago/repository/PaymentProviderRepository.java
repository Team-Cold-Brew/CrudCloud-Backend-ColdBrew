package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.mercadoPago.model.PaymentProvider;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Integer> {

    /**
     * Find payment provider by name
     */
    Optional<PaymentProvider> findByName(String name);

    /**
     * Find all active payment providers
     */
    @Query("SELECT p FROM PaymentProvider p WHERE p.active = true")
    List<PaymentProvider> findAllActive();

    /**
     * Check if provider exists by name
     */
    boolean existsByName(String name);

    /**
     * Find MercadoPago provider specifically
     */
    @Query("SELECT p FROM PaymentProvider p WHERE p.name = 'MERCADO_PAGO' AND p.active = true")
    Optional<PaymentProvider> findMercadoPagoProvider();
}
