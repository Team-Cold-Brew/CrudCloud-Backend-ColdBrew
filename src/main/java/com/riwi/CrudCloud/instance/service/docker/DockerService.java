package com.riwi.CrudCloud.instance.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.riwi.CrudCloud.instance.model.ENUM.DbType;
import com.riwi.CrudCloud.instance.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

    private final DockerClient dockerClient;
    private final InstanceRepository instanceRepository;

    // Range of ports that the application can assign to containers
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 20000;
    private static final Random RANDOM = new Random();

    // Mapping DBType to Docker image and default port
    private static final Map<DbType, DockerImageConfig> DB_CONFIGS = Map.of(
            DbType.MYSQL, new DockerImageConfig("mysql:8.0", 3306, "MYSQL_ROOT_PASSWORD"),
            DbType.POSTGRESQL, new DockerImageConfig("postgres:15", 5432, "POSTGRES_PASSWORD"),
            DbType.MONGODB, new DockerImageConfig("mongo:latest", 27017, null) // MongoDB does not require PASS by ENV by default.
    );

    private record DockerImageConfig(String image, int defaultPort, String passwordEnvKey) {}


    /**
     * Attempts to find an available port in the defined range.
     * @return Free port.
     */
    private int findAvailablePort() {
        AtomicInteger attempts = new AtomicInteger(0);
        while (attempts.get() < 100) {
            int port = RANDOM.nextInt(MAX_PORT - MIN_PORT + 1) + MIN_PORT;
            if (instanceRepository.findByPortAndDeletedAtIsNull(port).isEmpty()) {
                return port;
            }
            attempts.incrementAndGet();
        }
        throw new RuntimeException("No available port could be found after multiple attempts.");
    }

    /**
     * Creates and runs a Docker container for the specified DB.
     * @param dbType DB type.
     * @param instanceName Name for the container.
     * @param rootPassword Root password.
     * @return Container ID and assigned port.
     */
    public ContainerCreationResult createAndRunContainer(DbType dbType, String instanceName, String rootPassword) {

        DockerImageConfig config = DB_CONFIGS.get(dbType);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        }

        int hostPort = findAvailablePort();
        int containerPort = config.defaultPort;
        String containerName = "crudcloud-" + instanceName.toLowerCase().replaceAll("[^a-z0-9]", "-");

        ExposedPort exposedPort = ExposedPort.tcp(containerPort);
        PortBinding portBinding = new PortBinding(Binding.bindPort(hostPort), exposedPort);
        HostConfig hostConfig = new HostConfig()
                .withPortBindings(portBinding);

        String[] environment = config.passwordEnvKey != null ?
                new String[]{config.passwordEnvKey + "=" + rootPassword} :
                new String[]{};

        log.info("Creating container {} for image {} with HOST port:{}", containerName, config.image, hostPort);

        try {
            dockerClient.pullImageCmd(config.image)
                    .exec(new com.github.dockerjava.core.command.PullImageResultCallback())
                    .awaitSuccess();

            CreateContainerResponse container = dockerClient.createContainerCmd(config.image)
                    .withName(containerName)
                    .withExposedPorts(exposedPort)
                    .withHostConfig(hostConfig)
                    .withEnv(environment)
                    .withRestartPolicy(RestartPolicy.unlessStoppedRestart())
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            log.info("Container {} created and running with ID: {}", containerName, container.getId());

            return new ContainerCreationResult(container.getId(), hostPort);

        } catch (Exception e) {
            log.error("Error creating and running the Docker container: {}", e.getMessage());
            throw new RuntimeException("Failure to orchestrate the database container.", e);
        }
    }
    /**
     * Stops (suspends) a container.
     * @param containerId Container ID.
     */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("Container with ID {} stopped (suspended).", containerId);
        } catch (Exception e) {
            log.error("Error stopping the container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failure to stop the container.", e);
        }
    }

    /**
     * Starts (resumes) a container.
     * @param containerId Container ID.
     */
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container with ID {} started (restarted).", containerId);
        } catch (Exception e) {
            log.error("Error starting the container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Error starting container.", e);
        }
    }

    /**
     * Deletes a container.
     * @param containerId Container ID.
     */
    public void removeContainer(String containerId) {
        try {
            stopContainer(containerId);
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .withRemoveVolumes(true)
                    .exec();
            log.info("Container with ID {} deleted", containerId);
        } catch (Exception e) {
            log.error("Error deleting container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Error deleting container.", e);
        }
    }

    // Simple DTO to return the results of container creation
    public record ContainerCreationResult(String containerId, int hostPort) {}
}