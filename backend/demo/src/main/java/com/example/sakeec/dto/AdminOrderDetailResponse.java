package com.example.sakeec.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 管理画面: 注文詳細レスポンス。
 * 顧客情報・明細・合計・ステータス・受注日時を返す。
 */
public record AdminOrderDetailResponse(
    Long id,
    String orderNumber,
    String customerName,
    String customerEmail,
    String customerPhone,
    String deliveryAddress,
    BigDecimal totalAmount,
    String status,
    Instant createdAt,
    List<Item> items
) {
    public record Item(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
