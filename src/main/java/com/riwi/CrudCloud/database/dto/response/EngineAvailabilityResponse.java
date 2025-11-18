package com.riwi.CrudCloud.database.dto.response;

/**
 * Response DTO for engine availability status
 */
public class EngineAvailabilityResponse {

    private String name;
    private Boolean available;
    private String reason;

    public EngineAvailabilityResponse() {
    }

    public EngineAvailabilityResponse(String name, Boolean available, String reason) {
        this.name = name;
        this.available = available;
        this.reason = reason;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
