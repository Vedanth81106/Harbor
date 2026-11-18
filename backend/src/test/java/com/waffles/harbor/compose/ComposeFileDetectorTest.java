package com.waffles.harbor.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeFileDetectorTest {

    private final ComposeFileDetector detector = new ComposeFileDetector();

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectComposeFile() throws Exception {
        Path composeFile = tempDir.resolve("docker-compose.yml");
        Files.writeString(composeFile, "services: {}");

        assertEquals(
                Optional.of(composeFile),
                detector.detectComposeFiles(tempDir.toString())
        );
    }

    @Test
    void shouldPreferCanonicalComposeFileName() throws Exception {
        Path composeFile = tempDir.resolve("compose.yaml");
        Files.writeString(composeFile, "services: {}");
        Files.writeString(tempDir.resolve("compose.yml"), "services: {}");

        assertEquals(
                Optional.of(composeFile),
                detector.detectComposeFiles(tempDir.toString())
        );
    }

    @Test
    void shouldReturnEmptyWhenNoComposeFileExists() {
        assertTrue(
                detector.detectComposeFiles(tempDir.toString()).isEmpty()
        );
    }
}
