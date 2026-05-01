package com.example.sakeec.dto;

import java.util.List;
import java.util.Map;

public record ValidationErrorResponse(
    String code,
    String message,
    String traceId,
    Map<String, List<String>> errors
) {}
