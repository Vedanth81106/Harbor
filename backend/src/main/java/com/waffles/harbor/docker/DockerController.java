package com.waffles.harbor.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController{

    private final DockerService dockerService;

    @PostMapping("/nginx/{containerId}")
    public InspectContainerResponse inspectContainer(@PathVariable String containerId){
        return dockerService.inspectContainerResponse(containerId);
    }

    @PostMapping("/nginx/{containerId}/start")
    public void startContainer(@PathVariable String containerId) throws InterruptedException{
        dockerService.startContainer(containerId);
    }

    @PostMapping("/nginx/{containerId}/stop")
    public void stopContainer(@PathVariable String containerId){
        dockerService.stopContainer(containerId);
    }

    @DeleteMapping("/nginx/{containerId}/delete")
    public void deleteContainer(@PathVariable String containerId){
        dockerService.removeContainer(containerId);
    }
}
