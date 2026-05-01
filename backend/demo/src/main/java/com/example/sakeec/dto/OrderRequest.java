package com.example.sakeec.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderRequest(
    @NotEmpty @Valid List<Item> items,
    @NotNull @Valid Customer customer
) {
    public record Item(
        @NotNull Long productId,
        @NotNull @Min(1) @Max(3) Integer quantity
    ) {}

    public record Customer(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        @NotBlank String deliveryAddress
    ) {}
}
