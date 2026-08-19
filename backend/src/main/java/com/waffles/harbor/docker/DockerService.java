package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import org.springframework.stereotype.Service;

@Service
public class DockerService {

    private final DockerClient dockerClient;

    public DockerService(DockerClient dockerClient){
        this.dockerClient = dockerClient;
    }

    public String createNginxContainer() throws InterruptedException {
        dockerClient.pullImageCmd("nginx").withTag("latest").start().awaitCompletion();

        ExposedPort tcp80 = ExposedPort.tcp(80);
        Ports portBindings = new Ports();
        portBindings.bind(tcp80, Ports.Binding.bindPort(8081));

        CreateContainerResponse container = dockerClient.createContainerCmd("nginx")
                .withName("nginx-container")
                .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings))
                .exec();

        return container.getId();

    }

    public String startNginxContainer() throws InterruptedException {

        String containerId = createNginxContainer();
        dockerClient.startContainerCmd(containerId).exec();

        return containerId;
    }

    public String stopNginxContainer(String containerId){

        dockerClient.stopContainerCmd(containerId).exec();
        return containerId;
    }

    public void removeNginxContainer(String containerId){

        dockerClient.removeContainerCmd(containerId).exec();
    }

    public InspectContainerResponse inspectContainerResponse(String containerId) {

        // Contains container metadata, state, networking, ports,
        // environment, mounts, restart policy, command, exit code, and health.
        return dockerClient.inspectContainerCmd(containerId).exec();
    }
}
