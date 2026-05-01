package com.example.sakeec.dto;

import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String brewery,
    BigDecimal price,
    Integer volume,
    BigDecimal alcoholContent,
    String description,
    String imageUrl,
    Integer stockQuantity
) {}
