package com.waffles.harbor.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    private final GitService gitService = new GitService();

    @Test
    void shouldRejectNullRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository(null, "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository url is invalid!", exception.getMessage());
    }

    @Test
    void shouldRejectBlankRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("   ", "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository url is invalid!", exception.getMessage());
    }

    @Test
    void shouldRejectNonHttpsRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("http://github.com/user/repo.git", "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository url is invalid!", exception.getMessage());
    }

    @Test
    void shouldRejectRepositoryUrlWithCredentials() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository(
                        "https://user:password@github.com/user/repo.git",
                        "/tmp/does-not-matter",
                        60
                )
        );

        assertEquals("Repository url is invalid!", exception.getMessage());
    }

    @Test
    void shouldRejectMalformedRepositoryUrl() {
        assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("https:///repo.git", "/tmp/does-not-matter", 60)
        );
    }

    @Test
    void shouldRejectLoopbackRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("https://127.0.0.1/user/repo.git", "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository URL resolves to a restricted network address", exception.getMessage());
    }

    @Test
    void shouldRejectLocalhostRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("https://localhost/user/repo.git", "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository URL resolves to a restricted network address", exception.getMessage());
    }

    @Test
    void shouldRejectPrivateNetworkRepositoryUrl() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> gitService.cloneRepository("https://192.168.1.10/user/repo.git", "/tmp/does-not-matter", 60)
        );

        assertEquals("Repository URL resolves to a restricted network address", exception.getMessage());
    }

    @Test
    void shouldReturnLatestCommitFromRepository(@TempDir Path repositoryDirectory) throws Exception {
        RevCommit commit = createRepositoryWithCommit(repositoryDirectory, "initial commit");

        GitCommit latest = gitService.getLatestCommit(repositoryDirectory.toString());

        assertEquals(commit.getName(), latest.getHash());
        assertEquals("initial commit", latest.getMessage());
        assertEquals("Tester", latest.getAuthor());
        assertNotNull(latest.getDate());
    }

    @Test
    void shouldReturnAllCommitsFromRepository(@TempDir Path repositoryDirectory) throws Exception {
        createRepositoryWithCommit(repositoryDirectory, "initial commit");

        List<GitCommit> commits = gitService.getCommits(repositoryDirectory.toString());

        assertEquals(1, commits.size());
        assertEquals("initial commit", commits.getFirst().getMessage());
    }

    private RevCommit createRepositoryWithCommit(Path directory, String message) throws Exception {
        try (Git git = Git.init().setDirectory(directory.toFile()).call()) {
            Files.writeString(directory.resolve("file.txt"), "hello");

            git.add().addFilepattern("file.txt").call();

            return git.commit()
                    .setMessage(message)
                    .setAuthor("Tester", "tester@example.com")
                    .setCommitter("Tester", "tester@example.com")
                    .call();
        }
    }
}
