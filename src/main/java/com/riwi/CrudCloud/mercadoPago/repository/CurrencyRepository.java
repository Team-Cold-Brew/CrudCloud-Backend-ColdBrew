package com.riwi.CrudCloud.mercadoPago.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.mercadoPago.model.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {

    /**
     * Find currency by currency code (e.g., "ARS", "USD")
     */
    Optional<Currency> findByCurrency(String currency);

    /**
     * Check if currency exists by currency code
     */
    boolean existsByCurrency(String currency);
}
