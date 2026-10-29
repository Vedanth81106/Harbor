
package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DockerService {

    // TODO: Isolate Docker API access behind a restricted deployment worker/proxy.

    private static final String TRAEFIK_CONTAINER = "traefik";
    private static final String NETWORK_PREFIX = "net-";
    private static final long MEMORY_LIMIT = 512L * 1024 * 1024;
    private static final long CPU_PERIOD = 100_000L;
    private static final long CPU_QUOTA = 50_000L;
    private static final long PID_LIMIT = 100L;

    private final DockerClient dockerClient;

    public void buildImage(String filePath, String imageTag) {
        File buildDirectory = new File(filePath);
        File dockerfile = new File(buildDirectory, "Dockerfile");

        dockerClient.buildImageCmd()
                .withBaseDirectory(buildDirectory)
                .withDockerfile(dockerfile)
                .withTags(Collections.singleton(imageTag))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    public String createContainer(
            UUID deploymentId,
            String imageTag,
            int containerPort,
            String containerName
    ) {
        String networkName = getNetworkName(deploymentId);

        createIsolatedNetwork(networkName);

        try {
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNetworkMode(networkName)
                    .withPidsLimit(PID_LIMIT)
                    .withMemory(MEMORY_LIMIT)
                    .withCpuPeriod(CPU_PERIOD)
                    .withCpuQuota(CPU_QUOTA)
                    .withRestartPolicy(RestartPolicy.noRestart());

            Map<String, String> labels = createTraefikLabels(
                    networkName,
                    containerPort,
                    containerName
            );

            CreateContainerResponse response = dockerClient
                    .createContainerCmd(imageTag)
                    .withLabels(labels)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .exec();

            return response.getId();

        } catch (RuntimeException e) {
            removeNetworkIfPresent(networkName);
            throw e;
        }
    }

    public void startContainer(String containerId, UUID deploymentId) {
        String networkName = getNetworkName(deploymentId);

        // Fail rather than silently creating a deployment without routing.
        String traefikId = dockerClient
                .inspectContainerCmd(TRAEFIK_CONTAINER)
                .exec()
                .getId();

        dockerClient.connectToNetworkCmd()
                .withNetworkId(networkName)
                .withContainerId(traefikId)
                .exec();

        dockerClient.startContainerCmd(containerId).exec();
    }

    public void stopContainer(String containerId) {
        try {
            InspectContainerResponse container = dockerClient
                    .inspectContainerCmd(containerId)
                    .exec();

            if (Boolean.TRUE.equals(container.getState().getRunning())) {
                dockerClient.stopContainerCmd(containerId).exec();
            }
        } catch (NotFoundException e) {
            log.debug("Container {} is already absent.", containerId);
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            log.debug("Container {} is already absent.", containerId);
        }
    }

    public void removeImage(String imageName) {
        try {
            InspectImageResponse image = dockerClient
                    .inspectImageCmd(imageName)
                    .exec();

            dockerClient.removeImageCmd(image.getId()).exec();
        } catch (NotFoundException e) {
            log.debug("Image {} is already absent.", imageName);
        }
    }

    public int getExposedImagePort(String imageName) {
        InspectImageResponse image = dockerClient
                .inspectImageCmd(imageName)
                .exec();

        ExposedPort[] ports = image.getConfig().getExposedPorts();

        if (ports == null || ports.length == 0) {
            throw new IllegalStateException(
                    "Could not find an exposed port in image " + imageName
            );
        }

        return ports[0].getPort();
    }

    /**
     * Removes a deployment's container, Traefik network attachment,
     * and dedicated network. Safe to call after partial provisioning.
     */
    public void tearDownDeployment(String containerId, UUID deploymentId) {
        String networkName = getNetworkName(deploymentId);

        try {
            String traefikId = dockerClient
                    .inspectContainerCmd(TRAEFIK_CONTAINER)
                    .exec()
                    .getId();

            dockerClient.disconnectFromNetworkCmd()
                    .withContainerId(traefikId)
                    .withNetworkId(networkName)
                    .exec();

        } catch (NotFoundException e) {
            log.debug(
                    "Traefik or network attachment {} is already absent.",
                    networkName
            );
        } catch (RuntimeException e) {
            log.warn("Could not disconnect Traefik from {}.", networkName, e);
        }

        if (containerId != null) {
            try {
                dockerClient.removeContainerCmd(containerId)
                        .withForce(true)
                        .exec();
            } catch (NotFoundException e) {
                log.debug("Container {} is already absent.", containerId);
            } catch (RuntimeException e) {
                log.warn("Could not remove container {}.", containerId, e);
            }
        }

        removeNetworkIfPresent(networkName);
    }

    private void createIsolatedNetwork(String networkName) {
        dockerClient.createNetworkCmd()
                .withName(networkName)
                .withDriver("bridge")
                .exec();
    }

    private void removeNetworkIfPresent(String networkName) {
        try {
            dockerClient.removeNetworkCmd(networkName).exec();
        } catch (NotFoundException e) {
            log.debug("Network {} is already absent.", networkName);
        } catch (RuntimeException e) {
            log.warn(
                    "Could not remove network {}. It may still be in use.",
                    networkName,
                    e
            );
        }
    }

    private String getNetworkName(UUID deploymentId) {
        return NETWORK_PREFIX
                + deploymentId.toString().substring(0, 8);
    }

    private Map<String, String> createTraefikLabels(
            String networkName,
            int containerPort,
            String containerName
    ) {
        Map<String, String> labels = new HashMap<>();

        labels.put("traefik.enable", "true");
        labels.put("traefik.docker.network", networkName);
        labels.put(
                "traefik.http.routers." + containerName + ".entrypoints",
                "web"
        );
        labels.put(
                "traefik.http.routers." + containerName + ".rule",
                "Host(`" + containerName + ".localhost`)"
        );
        labels.put(
                "traefik.http.services." + containerName
                        + ".loadbalancer.server.port",
                String.valueOf(containerPort)
        );

        return labels;
    }
}
