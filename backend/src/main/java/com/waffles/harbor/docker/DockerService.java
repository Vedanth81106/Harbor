package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import com.waffles.harbor.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DockerService {

    // TODO: isolate Docker API behind a restricted deployment worker/proxy -> another phase by itself.

    private final DockerClient dockerClient;
    private final SseService sseService;

    public void buildImage(String filePath, String imageTag) {

        File buildDir = new File(filePath);
        File dockerfile = new File(buildDir, "Dockerfile");

        dockerClient.buildImageCmd()
                .withBaseDirectory(buildDir)
                .withDockerfile(dockerfile)
                .withTags(Collections.singleton(imageTag))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }

    public String createContainer(String imageTag, int hostPort, int containerPort, String containerName){

        ExposedPort internalPort = new ExposedPort(containerPort);
        Ports portBindings = new Ports();
        portBindings.bind(internalPort, Ports.Binding.bindPort(hostPort));
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPidsLimit(100L)
                .withMemory(512*1024*1024L) // max 512Mb RAM
                .withCpuPeriod(100000L)
                .withCpuQuota(50000L)// container can only use 0.5% of cpu within the specified timeframe(100000 microseconds) i.e. 50% of one core
                .withPortBindings(portBindings)
                .withRestartPolicy(RestartPolicy.noRestart());

        // TODO: disk and network are not limited. make sure to take care of them in the future tho

        CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageTag)
                .withName(containerName)
                .withHostConfig(hostConfig)
                .exec();

        return containerResponse.getId();
    }

    public void startContainer(String containerId) throws InterruptedException {

        dockerClient.startContainerCmd(containerId).exec();
    }

    public void stopContainer(String containerId) {

        try {
            InspectContainerResponse container =
                    dockerClient.inspectContainerCmd(containerId).exec();

            if (Boolean.TRUE.equals(container.getState().getRunning())) {
                dockerClient.stopContainerCmd(containerId).exec();
            }

        } catch (NotFoundException e) {
            System.out.println(
                    "Container " + containerId + " already removed."
            );
        }
    }

    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            System.out.println("Container " + containerId + " already removed.");
        }
    }

    public void removeImage(String imageName) {
        try {
            InspectImageResponse image = dockerClient.inspectImageCmd(imageName).exec();
            dockerClient.removeImageCmd(image.getId()).exec();

        } catch (NotFoundException e) {
            System.out.println("Image " + imageName + " does not exist.");
        }
    }

    public int getExposedImagePort(String imageName){

        InspectImageResponse imageResponse = dockerClient.inspectImageCmd(imageName).exec();
        ExposedPort[] ports = imageResponse.getConfig().getExposedPorts();

        if (ports == null || ports.length == 0) {
            throw new IllegalStateException(
                    "Could not find a port to expose from the docker image!"
            );
        }

        return ports[0].getPort();
    }

    public int getDockerContainerPort(String containerId, int containerPort){

        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerId).exec();
        Ports bindings = containerResponse.getNetworkSettings().getPorts();
        if(bindings == null || bindings.getBindings() == null) throw new IllegalStateException("No bindings found for container");

        Ports.Binding[] hostBinding = bindings.getBindings().get(ExposedPort.tcp(containerPort));
        if(hostBinding == null || hostBinding.length == 0) throw new IllegalStateException("Could not find port binding!");

        return Integer.parseInt(hostBinding[0].getHostPortSpec());
    }
}
