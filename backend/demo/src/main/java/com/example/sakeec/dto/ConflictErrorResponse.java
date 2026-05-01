package com.example.sakeec.dto;

public record ConflictErrorResponse(
    String code,
    String message,
    String currentStatus
) {}
