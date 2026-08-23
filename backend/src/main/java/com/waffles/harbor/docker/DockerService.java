package com.waffles.harbor.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
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

        System.out.println("Build dir: " + buildDir.getAbsolutePath());
        System.out.println("Build dir exists: " + buildDir.exists());
        System.out.println("Build dir is directory: " + buildDir.isDirectory());
        System.out.println("Dockerfile: " + dockerfile.getAbsolutePath());
        System.out.println("Dockerfile exists: " + dockerfile.exists());
        System.out.println("Dockerfile is file: " + dockerfile.isFile());

        dockerClient.buildImageCmd()
                .withBaseDirectory(buildDir)
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

    public void stopContainer(String containerId){

        dockerClient.stopContainerCmd(containerId).exec();
    }

    public void removeContainer(String containerId){

        dockerClient.removeContainerCmd(containerId).exec();
    }

    public InspectContainerResponse inspectContainerResponse(String containerId) {

        // Contains container metadata, state, networking, ports,
        // environment, mounts, restart policy, command, exit code, and health.
        return dockerClient.inspectContainerCmd(containerId).exec();
    }
}
