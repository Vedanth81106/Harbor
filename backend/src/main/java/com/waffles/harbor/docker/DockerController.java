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

    @PostMapping("/nginx/start")
    public String startNginxContainer() throws InterruptedException{
        return dockerService.startNginxContainer();
    }

    @PostMapping("/nginx/{containerId}/stop")
    public String stopNginxContainer(@PathVariable String containerId){
        return dockerService.stopNginxContainer(containerId);
    }

    @DeleteMapping("/nginx/{containerId}/delete")
    public void deleteNginxContainer(@PathVariable String containerId){
        dockerService.removeNginxContainer(containerId);
    }
}
