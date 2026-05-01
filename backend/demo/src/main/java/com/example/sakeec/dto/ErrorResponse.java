package com.example.sakeec.dto;

public record ErrorResponse(
    String code,
    String message,
    String traceId
) {}
