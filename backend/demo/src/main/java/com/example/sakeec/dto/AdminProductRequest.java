package com.example.sakeec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdminProductRequest(
    @NotBlank String name,
    @NotNull BigDecimal price,
    String imageUrl
) {}
