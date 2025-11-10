package com.riwi.CrudCloud.auth.dto.response;

import java.math.BigDecimal;

public class PlanResponse {

    private Integer planId;
    private String name;
    private String description;
    private Integer maxInstances;
    private BigDecimal price;
    private String billingCycle;

    // Constructor
    public PlanResponse(Integer planId, String name, String description, Integer maxInstances,
                       BigDecimal price, String billingCycle) {
        this.planId = planId;
        this.name = name;
        this.description = description;
        this.maxInstances = maxInstances;
        this.price = price;
        this.billingCycle = billingCycle;
    }

    // Getters and Setters
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(Integer maxInstances) {
        this.maxInstances = maxInstances;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }
}
