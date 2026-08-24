package com.waffles.harbor.git;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GitCommit {

    private String hash;
    private String message;
    private String author;
    private Instant date;
}
