package com.riwi.CrudCloud.instance.service;

import com.riwi.CrudCloud.instance.exception.CustomBadRequestException;
import com.riwi.CrudCloud.instance.exception.CustomNotFoundException;
import com.riwi.CrudCloud.instance.util.EncryptionUtil;
import com.riwi.CrudCloud.instance.util.MailService;
import com.riwi.CrudCloud.instance.util.PdfGeneratorService;

import com.riwi.CrudCloud.instance.dto.InstanceCreateRequest;
import com.riwi.CrudCloud.instance.dto.InstanceResponse;
import com.riwi.CrudCloud.instance.model.Instance;
import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import com.riwi.CrudCloud.instance.model.ENUM.InstanceStatus;
import com.riwi.CrudCloud.instance.model.Plan;
import com.riwi.CrudCloud.instance.model.User;
import com.riwi.CrudCloud.instance.repository.InstanceRepository;
import com.riwi.CrudCloud.instance.repository.PlanRepositoryByInstance;
import com.riwi.CrudCloud.instance.repository.UserRepositoryByIntance;
import com.riwi.CrudCloud.instance.service.docker.DockerService;
import com.riwi.CrudCloud.instance.service.docker.DockerService.ContainerCreationResult;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final UserRepositoryByIntance userRepository;
    private final PlanRepositoryByInstance planRepository;
    private final DockerService dockerService;
    private final MailService mailService;
    private final PdfGeneratorService pdfGeneratorService;

    @Value("${crudcloud.host.address:localhost}")
    private String hostAddress;

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 16);
    }

    private void validateInstanceLimit(Long userId, Long organizationId, Plan plan) {
        Long currentCount;
        if (organizationId != null) {
            currentCount = instanceRepository.countActiveByOrganizationId(organizationId);
        } else {
            currentCount = instanceRepository.countActiveByUserId(userId);
        }

        if (currentCount >= plan.getMaxInstances()) {
            throw new CustomBadRequestException("Instance limit reached. Your plan (" + plan.getName() + ") allows up to " + plan.getMaxInstances() + " instances.");
        }
    }

    private String generateFreeInstanceName(DbType dbType) {
        return dbType.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create a new database instance (Docker container).
     */
    @Transactional
    public InstanceResponse createInstance(InstanceCreateRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomNotFoundException("User not found."));

        Plan plan = planRepository.findById(user.getPersonalPlanId())
                .orElseThrow(() -> new CustomNotFoundException("Plan not found for the user."));

        validateInstanceLimit(request.getUserId(), request.getOrganizationId(), plan);

        // Generate credential
        String rawPassword = generateRandomPassword();
        String encryptedPassword = EncryptionUtil.encrypt(rawPassword);
        String username = request.getDbType().name().toLowerCase() + "_user";

        String instanceName = plan.getName().equalsIgnoreCase("Free") || request.getName() == null
                ? generateFreeInstanceName(request.getDbType())
                : request.getName();

        ContainerCreationResult dockerResult = dockerService.createAndRunContainer(
                request.getDbType(),
                instanceName,
                rawPassword
        );


        Instance newInstance = Instance.builder()
                .name(instanceName)
                .user(user)
                .organization(request.getOrganizationId() != null ? null : null)
                .status(InstanceStatus.RUNNING)
                .dbType(request.getDbType())
                .host(hostAddress)
                .port(dockerResult.hostPort())
                .username(username)
                .password(encryptedPassword)
                .containerId(dockerResult.containerId())
                .pdfDownloadStatus(false)
                .build();

        newInstance = instanceRepository.save(newInstance);

        // Send Email (No password in plain text)
        mailService.sendInstanceCreationEmail(newInstance, username, hostAddress, dockerResult.hostPort());

        // Return DTO (No password)
        return InstanceResponse.builder()
                .instanceId(newInstance.getInstanceId())
                .name(newInstance.getName())
                .userId(newInstance.getUser().getUserId())
                .status(newInstance.getStatus())
                .dbType(newInstance.getDbType())
                .host(newInstance.getHost())
                .port(newInstance.getPort())
                .username(newInstance.getUsername())
                .createdAt(newInstance.getCreatedAt())
                .build();
    }


    /**
     * Gets the credentials PDF, decrypts the password, and updates the download status.
     * The password is only visible in plain text during this process.
     * @param instanceId Instance ID.
     * @return The PDF as a byte array.
     */
    @Transactional
    public byte[] downloadCredentialsPdf(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        // Check if it has already been downloaded (Business rule: “only displayed once”)
        if (instance.getPdfDownloadStatus()) {
            throw new CustomBadRequestException("The credentials for this instance have already been downloaded. Please reset your password if you have lost them.");
        }

        String rawPassword = EncryptionUtil.decrypt(instance.getPassword());

        // Generate the PDF (regenerated with the RAW password)
        byte[] pdfBytes = pdfGeneratorService.generateInstanceCredentialsPdf(instance, rawPassword);

        // Mark as downloaded (Only done once!)
        instance.setPdfDownloadStatus(true);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        return pdfBytes;
    }

    /**
     * Suspends an instance (stops the container).
     */
    @Transactional
    public InstanceResponse suspendInstance(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        if (instance.getStatus() == InstanceStatus.SUSPENDED) {
            throw new CustomBadRequestException("The instance is already suspended.");
        }

        dockerService.stopContainer(instance.getContainerId());

        instance.setStatus(InstanceStatus.SUSPENDED);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        return InstanceResponse.builder()
                .instanceId(instance.getInstanceId())
                .name(instance.getName())
                .userId(instance.getUser().getUserId())
                .status(instance.getStatus())
                .dbType(instance.getDbType())
                .host(instance.getHost())
                .port(instance.getPort())
                .username(instance.getUsername())
                .createdAt(instance.getCreatedAt())
                .build();
    }


    /**
     * Resumes an instance (starts the container).
     */
    @Transactional
    public InstanceResponse resumeInstance(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        if (instance.getStatus() == InstanceStatus.RUNNING) {
            throw new CustomBadRequestException("The instance is already running.");
        }

        dockerService.startContainer(instance.getContainerId());

        instance.setStatus(InstanceStatus.RUNNING);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        return InstanceResponse.builder()
                .instanceId(instance.getInstanceId())
                .name(instance.getName())
                .userId(instance.getUser().getUserId())
                .status(instance.getStatus())
                .dbType(instance.getDbType())
                .host(instance.getHost())
                .port(instance.getPort())
                .username(instance.getUsername())
                .createdAt(instance.getCreatedAt())
                .build();
    }

    /**
     * Deletes an instance (stops and deletes the container, and applies soft delete in the database).
     */
    @Transactional
    public void deleteInstance(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        dockerService.removeContainer(instance.getContainerId());

        instance.setStatus(InstanceStatus.DELETED);
        instance.setDeletedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);
    }

    /**
     * Rotates the instance password, updates the DB, and notifies.
     */
    @Transactional
    public InstanceResponse rotatePassword(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        String newRawPassword = generateRandomPassword();
        String newEncryptedPassword = EncryptionUtil.encrypt(newRawPassword);

        instance.setPassword(newEncryptedPassword);
        instance.setPdfDownloadStatus(false);
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        // Generate new PDF and send email
        pdfGeneratorService.generateInstanceCredentialsPdf(instance, newRawPassword);
        mailService.sendPasswordRotationEmail(instance, newRawPassword);

        return InstanceResponse.builder()
                .instanceId(instance.getInstanceId())
                .name(instance.getName())
                .userId(instance.getUser().getUserId())
                .status(instance.getStatus())
                .dbType(instance.getDbType())
                .host(instance.getHost())
                .port(instance.getPort())
                .username(instance.getUsername())
                .createdAt(instance.getCreatedAt())
                .build();
    }

    /**
     * Lists all active instances for a user (or organization).
     */
    public List<InstanceResponse> getAllUserInstances(Long userId) {

        return instanceRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(instance -> InstanceResponse.builder()
                        .instanceId(instance.getInstanceId())
                        .name(instance.getName())
                        .userId(instance.getUser().getUserId())
                        .status(instance.getStatus())
                        .dbType(instance.getDbType())
                        .host(instance.getHost())
                        .port(instance.getPort())
                        .username(instance.getUsername())
                        .createdAt(instance.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Gets the details of an instance.
     */
    public InstanceResponse getInstanceDetails(Long instanceId) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new CustomNotFoundException("Instance not found."));

        return InstanceResponse.builder()
                .instanceId(instance.getInstanceId())
                .name(instance.getName())
                .userId(instance.getUser().getUserId())
                .status(instance.getStatus())
                .dbType(instance.getDbType())
                .host(instance.getHost())
                .port(instance.getPort())
                .username(instance.getUsername())
                .createdAt(instance.getCreatedAt())
                .build();
    }
}