package com.riwi.CrudCloud.database.repository;

import com.riwi.CrudCloud.common.models.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepositoryByInstance extends JpaRepository<Plan, Integer> {}