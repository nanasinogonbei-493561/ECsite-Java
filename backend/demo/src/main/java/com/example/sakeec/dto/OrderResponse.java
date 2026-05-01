package com.example.sakeec.dto;

import java.math.BigDecimal;

public record OrderResponse(
    String orderNumber,
    BigDecimal totalAmount,
    String status
) {}
