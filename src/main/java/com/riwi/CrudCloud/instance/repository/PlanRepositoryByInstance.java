package com.riwi.CrudCloud.instance.repository;


import com.riwi.CrudCloud.instance.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepositoryByInstance extends JpaRepository<Plan, Integer> {}