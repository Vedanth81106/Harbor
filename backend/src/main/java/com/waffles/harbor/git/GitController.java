package com.waffles.harbor.git;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/git")
@RequiredArgsConstructor
public class GitController{

    private final GitService gitService;

    @PostMapping("/clone")
    public String cloneRepository(@RequestParam String repoUrl) throws Exception {

        String filePath = "/harbor-tmp/";
        gitService.cloneRepository(repoUrl, filePath);
        return "Git repo successfully cloned!";
    }
}
