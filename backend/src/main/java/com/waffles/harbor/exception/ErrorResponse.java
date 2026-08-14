package com.waffles.harbor.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    int status;
    String message;
}
