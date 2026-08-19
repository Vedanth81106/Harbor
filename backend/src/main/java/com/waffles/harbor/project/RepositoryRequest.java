package com.waffles.harbor.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RepositoryRequest {

    @NotBlank(message = "Repository URL cannot be blank!")
    private String repositoryUrl;
}
