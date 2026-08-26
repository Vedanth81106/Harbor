package com.waffles.harbor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class GitService {

    public void cloneRepository(String repoUrl, String destination) throws Exception {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(destination))
                .call();
    }

    public List<GitCommit> getCommits(String localRepoPath) throws Exception {

        List<GitCommit> commits = new ArrayList<>();

        try (Git git = Git.open(new File(localRepoPath))) {

            for (RevCommit commit : git.log().call()) {

                commits.add(
                        GitCommit.builder()
                                .hash(commit.getName())
                                .message(commit.getShortMessage())
                                .author(commit.getAuthorIdent().getName())
                                .date(commit.getAuthorIdent().getWhen().toInstant())
                                .build()
                );
            }
        }

        return commits;
    }

    public GitCommit getLatestCommit(String localRepoPath) throws Exception {
        try (Git git = Git.open(new File(localRepoPath))) {

            RevCommit commit = git.log()
                    .setMaxCount(1)
                    .call()
                    .iterator()
                    .next();

            return GitCommit.builder()
                    .hash(commit.getName())
                    .message(commit.getShortMessage())
                    .author(commit.getAuthorIdent().getName())
                    .date(commit.getAuthorIdent().getWhen().toInstant())
                    .build();
        }
    }

}