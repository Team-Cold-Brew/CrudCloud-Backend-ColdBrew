package com.riwi.CrudCloud.instance.controller;

import com.riwi.CrudCloud.instance.dto.InstanceCreateRequest;
import com.riwi.CrudCloud.instance.dto.InstanceResponse;
import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import com.riwi.CrudCloud.instance.service.InstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    /**
     * Creates a new database instance (Docker container).
     * Requires plan limit validation in the Service.
     */
    @PostMapping
    public ResponseEntity<InstanceResponse> createInstance(@Valid @RequestBody InstanceCreateRequest request) {
        InstanceResponse response = instanceService.createInstance(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Lists all active instances for a user.
     * In a real system, the userId would be obtained from the security token.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InstanceResponse>> listUserInstances(@PathVariable Long userId) {
        List<InstanceResponse> instances = instanceService.getAllUserInstances(userId);
        return ResponseEntity.ok(instances);
    }


    @GetMapping("/{instanceId}")
    public ResponseEntity<InstanceResponse> getInstanceDetails(@PathVariable Long instanceId) {
        InstanceResponse response = instanceService.getInstanceDetails(instanceId);
        return ResponseEntity.ok(response);
    }


    /**
     * Suspends an instance (stops the container).
     */
    @PatchMapping("/{instanceId}/suspend")
    public ResponseEntity<InstanceResponse> suspendInstance(@PathVariable Long instanceId) {
        InstanceResponse response = instanceService.suspendInstance(instanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resumes an instance (starts the container).
     */
    @PatchMapping("/{instanceId}/resume")
    public ResponseEntity<InstanceResponse> resumeInstance(@PathVariable Long instanceId) {
        InstanceResponse response = instanceService.resumeInstance(instanceId);
        return ResponseEntity.ok(response);
    }

    /**
     * Rotates the password for an instance.
     */
    @PatchMapping("/{instanceId}/rotate-password")
    public ResponseEntity<InstanceResponse> rotatePassword(@PathVariable Long instanceId) {
        InstanceResponse response = instanceService.rotatePassword(instanceId);
        return ResponseEntity.ok(response);
    }


    /**
     * Deletes an instance (soft delete and removal from the container).
     */
    @DeleteMapping("/{instanceId}")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long instanceId) {
        instanceService.deleteInstance(instanceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Download the PDF with the credentials (Host, Port, User, RAW Password).
     * This action marks the instance as “PDF downloaded” (can only be done once).
     */
    @GetMapping("/{instanceId}/credentials-pdf")
    public ResponseEntity<ByteArrayResource> downloadCredentialsPdf(@PathVariable Long instanceId) {

        byte[] pdfBytes = instanceService.downloadCredentialsPdf(instanceId);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "crudcloud_credentials_" + instanceId + ".pdf");

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
}