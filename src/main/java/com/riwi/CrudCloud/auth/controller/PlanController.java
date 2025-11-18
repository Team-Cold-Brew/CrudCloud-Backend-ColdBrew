package com.riwi.CrudCloud.auth.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.auth.dto.response.PlanResponse;
import com.riwi.CrudCloud.auth.service.PlanService;

/**
 * Controller for plan management endpoints
 */
@RestController
@RequestMapping("/api/v1/plans")
@CrossOrigin(origins = "*")
public class PlanController {

    @Autowired
    private PlanService planService;

    /**
     * Get all available plans
     * GET /api/plans
     *
     * @return ResponseEntity with list of PlanResponse
     */
    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        List<PlanResponse> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Get user's current plan (defaults to FREE)
     * GET /api/v1/plans/current
     *
     * @return ResponseEntity with current PlanResponse
     */
    @GetMapping("/current")
    public ResponseEntity<PlanResponse> getCurrentPlan() {
        try {
            // Try to fetch FREE plan from database
            PlanResponse plan = planService.getPlanByName("FREE");
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            // Fallback: return hardcoded FREE plan if not found
            PlanResponse defaultPlan = new PlanResponse(
                1,
                "FREE",
                "Free tier plan",
                2,
                BigDecimal.ZERO,
                "monthly"
            );
            return ResponseEntity.ok(defaultPlan);
        }
    }

    /**
     * Get plan by name
     * GET /api/v1/plans/name/{name}
     *
     * @param name the plan name
     * @return ResponseEntity with PlanResponse
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<PlanResponse> getPlanByName(@PathVariable String name) {
        PlanResponse plan = planService.getPlanByName(name);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get plan by ID
     * GET /api/plans/{planId}
     *
     * @param planId the plan ID
     * @return ResponseEntity with PlanResponse
     */
    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable Integer planId) {
        PlanResponse plan = planService.getPlanById(planId);
        return ResponseEntity.ok(plan);
    }
}
