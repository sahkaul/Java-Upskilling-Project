package com.example.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiResponse", description = "Schema for API responses")
public class ApiResponse {

    private Boolean success;

    private String message;

    private String correlationId;

    private Object data;

    private String errorCode;
}

