package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;

@Service
public class DockerService {

    private final DockerClient dockerClient;

    public DockerService(DockerClient dockerClient){
        this.dockerClient = dockerClient;
    }

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

        CreateContainerResponse containerResponse = dockerClient.createContainerCmd(imageTag)
                .withName(containerName)
                .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings))
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

    public InspectContainerResponse inspectContainerResponse(String containerId) {

        // Contains container metadata, state, networking, ports,
        // environment, mounts, restart policy, command, exit code, and health.
        return dockerClient.inspectContainerCmd(containerId).exec();
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
