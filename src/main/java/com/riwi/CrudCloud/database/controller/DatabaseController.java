package com.riwi.CrudCloud.database.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riwi.CrudCloud.auth.service.PlanService;
import com.riwi.CrudCloud.common.models.DbType;
import com.riwi.CrudCloud.database.config.EngineProviderConfig;
import com.riwi.CrudCloud.database.dto.DatabaseCreateRequest;
import com.riwi.CrudCloud.database.dto.DatabaseResponse;
import com.riwi.CrudCloud.database.dto.response.EngineAvailabilityResponse;
import com.riwi.CrudCloud.database.service.DatabaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/instances")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseService databaseService;
    private final EngineProviderConfig engineProviderConfig;
    private final PlanService planService;

    /**
     * Creates a new database instance (DB inside a shared container).
     * Requires plan limit validation in the Service.
     */
    @PostMapping
    public ResponseEntity<DatabaseResponse> createDatabase(@Valid @RequestBody DatabaseCreateRequest request) {
        DatabaseResponse response = databaseService.createDatabase(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Lists all active databases for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DatabaseResponse>> listUserDatabases(@PathVariable Long userId) {
        List<DatabaseResponse> databases = databaseService.getAllUserDatabases(userId);
        return ResponseEntity.ok(databases);
    }


    @GetMapping("/{databaseId}")
    public ResponseEntity<DatabaseResponse> getDatabaseDetails(@PathVariable Long databaseId) {
        DatabaseResponse response = databaseService.getDatabaseDetails(databaseId);
        return ResponseEntity.ok(response);
    }


    /**
     * Suspends a database (revokes user connection permissions).
     */
    @PatchMapping("/{databaseId}/suspend")
    public ResponseEntity<DatabaseResponse> suspendDatabase(@PathVariable Long databaseId) {
        DatabaseResponse response = databaseService.suspendDatabase(databaseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resumes a database (restores user connection permissions).
     */
    @PatchMapping("/{databaseId}/resume")
    public ResponseEntity<DatabaseResponse> resumeDatabase(@PathVariable Long databaseId) {
        DatabaseResponse response = databaseService.resumeDatabase(databaseId);
        return ResponseEntity.ok(response);
    }

    /**
     * Rotates the password for a database user.
     */
    @PatchMapping("/{databaseId}/rotate-password")
    public ResponseEntity<DatabaseResponse> rotatePassword(@PathVariable Long databaseId) {
        DatabaseResponse response = databaseService.rotatePassword(databaseId);
        return ResponseEntity.ok(response);
    }


    /**
     * Deletes a database (drops DB and user in the container + soft delete).
     */
    @DeleteMapping("/{databaseId}")
    public ResponseEntity<Void> deleteDatabase(@PathVariable Long databaseId) {
        databaseService.deleteDatabase(databaseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download the PDF with the credentials.
     * This action marks the database as "PDF downloaded" (can only be done once).
     */
    @GetMapping("/{databaseId}/credentials-pdf")
    public ResponseEntity<ByteArrayResource> downloadCredentialsPdf(@PathVariable Long databaseId) {

        byte[] pdfBytes = databaseService.downloadCredentialsPdf(databaseId);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "crudcloud_credentials_" + databaseId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    /**
     * Returns the catalog of engines available for the Frontend.
     */
    @GetMapping("/catalog")
    public ResponseEntity<DbType[]> getAvailableDatabases() {
        return ResponseEntity.ok(DbType.values());
    }

    /**
     * Get engine availability considering provider config and user plan limits.
     * GET /api/v1/instances/available-engines
     *
     * @return ResponseEntity with list of EngineAvailabilityResponse
     */
    @GetMapping("/available-engines")
    public ResponseEntity<List<EngineAvailabilityResponse>> getAvailableEngines() {
        List<EngineAvailabilityResponse> engines = new ArrayList<>();

        for (DbType engine : DbType.values()) {
            boolean isAvailable = true;
            String reason = null;

            // Check 1: Is this engine configured by the provider?
            if (!engineProviderConfig.isEngineAvailable(engine)) {
                isAvailable = false;
                reason = "This engine is not configured";
            }

            engines.add(new EngineAvailabilityResponse(
                engine.name(),
                isAvailable,
                reason
            ));
        }

        return ResponseEntity.ok(engines);
    }
}