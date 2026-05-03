package com.example.sakeec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AdminProductRequest(
    @NotBlank String name,
    @NotNull BigDecimal price,
    String imageUrl,
    /** 在庫数 (0 以上)。null なら 0 として扱う。 */
    @PositiveOrZero Integer stockQuantity
) {}
