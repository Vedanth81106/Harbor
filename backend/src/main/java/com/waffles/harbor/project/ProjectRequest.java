package com.waffles.harbor.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProjectRequest {

    @NotBlank(message = "Project name cannot be blank!")
    @Size(max = 100)
    private String projectName;
}
