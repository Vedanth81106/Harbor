package com.waffles.harbor.git;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GitService {

    public void cloneRepository(String repoUrl, String destination, int timeout) throws Exception {

        if(!validateRepoUrl(repoUrl)) throw new IllegalStateException("Repository url is invalid!");
        if(!validateResolvedUrl(repoUrl)) throw new IllegalStateException("Repository URL resolves to a restricted network address");
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(destination))
                .setTimeout(timeout)
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

    private boolean validateRepoUrl(String repoUrl){
        if(repoUrl == null || repoUrl.isBlank()){
            return false;
        }
        try{
            URI uri = new URI(repoUrl);
            String scheme = uri.getScheme(); // gets the part before colon(i.e: http://, ftp://)

            if(scheme == null || !scheme.equalsIgnoreCase("https")) return false;
            if(uri.getHost() == null || uri.getHost().isBlank()) return false;
            if(uri.getUserInfo() != null){ // this prevents stuff like https://username:password@github.com from getting through.
                // userInfo gets the username:password part
                log.warn("Rejected URL because it might contains important credentials!");
                return false;
            }
        }catch (URISyntaxException uriSyntaxException){
            log.warn("Rejected malformed URL");
            return false;
        }

        return true;
    }

    private boolean validateResolvedUrl(String repoUrl){

        try {
            URI uri = new URI(repoUrl);
            String host = uri.getHost();
            InetAddress[] addresses = InetAddress.getAllByName(host);

            for(InetAddress address : addresses){
                if(address.isAnyLocalAddress() || address.isSiteLocalAddress() ||
                        address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isMulticastAddress()) return false;
            }
        } catch (URISyntaxException | UnknownHostException e) {
            return false;
        }

        return true;
    }
}