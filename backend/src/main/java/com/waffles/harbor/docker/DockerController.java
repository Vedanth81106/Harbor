package com.waffles.harbor.docker;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/docker")
@RequiredArgsConstructor
public class DockerController{

    private final DockerService dockerService;

    @PostMapping("/nginx/start")
    public String startNginxContainer() throws InterruptedException{
        return dockerService.startNginxContainer();
    }

    @PostMapping("/nginx/{containerId}/stop")
    public String stopNginxContainer(@PathVariable String containerId) throws InterruptedException{
        return dockerService.stopNginxContainer(containerId);
    }

    @DeleteMapping("/nginx/{containerId}/delete")
    public void deleteNginxContainer(@PathVariable String containerId) throws InterruptedException{
        dockerService.removeNginxContainer(containerId);
    }
}
