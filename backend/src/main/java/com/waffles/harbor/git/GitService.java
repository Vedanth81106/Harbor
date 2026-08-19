package com.waffles.harbor.git;

import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class GitService {

    public void cloneRepository(String repoUrl, String destination) throws Exception {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(destination))
                .call();
    }
}