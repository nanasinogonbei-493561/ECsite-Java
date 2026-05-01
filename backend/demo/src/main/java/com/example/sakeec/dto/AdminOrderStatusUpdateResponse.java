package com.example.sakeec.dto;

import java.time.Instant;

public record AdminOrderStatusUpdateResponse(
    Long id,
    String status,
    Instant updatedAt
) {}
