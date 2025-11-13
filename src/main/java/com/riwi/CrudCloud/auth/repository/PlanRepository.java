package com.riwi.CrudCloud.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.riwi.CrudCloud.common.models.Plan;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    /**
     * Find plan by name
     *
     * @param name the plan name
     * @return Optional containing the plan if found
     */
    Optional<Plan> findByName(String name);
}
