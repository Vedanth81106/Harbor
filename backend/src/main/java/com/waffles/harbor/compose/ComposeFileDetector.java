package com.waffles.harbor.compose;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
public class ComposeFileDetector {

    private static final List<String> COMPOSE_INDICATORS = List.of(
            "compose.yaml",
            "compose.yml",
            "docker-compose.yaml",
            "docker-compose.yml"
    );

    public Optional<Path> detectComposeFiles(String rootDir){

        Path root = Path.of(rootDir);

        for(String indicator : COMPOSE_INDICATORS){

            Path composeFile = root.resolve(indicator);
            if(Files.isRegularFile(composeFile)){
                return Optional.of(composeFile);
            }
        }

        return Optional.empty();
    }
}
