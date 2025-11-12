package com.riwi.CrudCloud.auth.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riwi.CrudCloud.auth.dto.response.PlanResponse;
import com.riwi.CrudCloud.auth.model.Plan;
import com.riwi.CrudCloud.auth.repository.PlanRepository;
import com.riwi.CrudCloud.auth.util.exception.classes.ResourceNotFoundException;

/**
 * Service class for plan management
 */
@Service
public class PlanService {

    @Autowired
    private PlanRepository planRepository;

    /**
     * Get all available plans
     *
     * @return List of PlanResponse
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findAll()
            .stream()
            .map(this::mapToPlanResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get plan by ID
     *
     * @param planId the plan ID
     * @return PlanResponse
     * @throws ResourceNotFoundException if plan not found
     */
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(Integer planId) {
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with ID: " + planId));

        return mapToPlanResponse(plan);
    }

    /**
     * Get plan by name
     *
     * @param name the plan name
     * @return PlanResponse
     * @throws ResourceNotFoundException if plan not found
     */
    @Transactional(readOnly = true)
    public PlanResponse getPlanByName(String name) {
        Plan plan = planRepository.findByName(name)
            .orElseThrow(() -> new ResourceNotFoundException("Plan not found with name: " + name));

        return mapToPlanResponse(plan);
    }

    /**
     * Map Plan entity to PlanResponse DTO
     *
     * @param plan the plan entity
     * @return PlanResponse
     */
    private PlanResponse mapToPlanResponse(Plan plan) {
        return new PlanResponse(
            plan.getPlanId(),
            plan.getName(),
            plan.getDescription(),
            plan.getMaxInstances(),
            plan.getPrice(),
            plan.getBillingCycle()
        );
    }
}
