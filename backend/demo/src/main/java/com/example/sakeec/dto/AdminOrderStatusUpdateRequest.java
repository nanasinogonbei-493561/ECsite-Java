package com.example.sakeec.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminOrderStatusUpdateRequest(
    @NotBlank String status
) {}
