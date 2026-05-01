package com.example.sakeec.controller;

import com.example.sakeec.dto.OrderResponse;
import com.example.sakeec.exception.GlobalExceptionHandler;
import com.example.sakeec.exception.OutOfStockException;
import com.example.sakeec.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // SecurityFilterChain を切り、Controller のロジックに集中
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper om;

    @MockitoBean private OrderService orderService;

    private String validBody() throws Exception {
        return om.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productId", 1, "quantity", 2)},
                "customer", Map.of(
                        "name", "田中",
                        "email", "tanaka@example.com",
                        "phone", "090-1111-2222",
                        "deliveryAddress", "東京都"
                )
        ));
    }

    @Test
    @DisplayName("正常: 201 Created と Location ヘッダ、body に orderNumber")
    void created() throws Exception {
        when(orderService.createOrder(any()))
                .thenReturn(new OrderResponse("A26ABC123", new BigDecimal("6000.00"), "PENDING"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/orders/A26ABC123")))
                .andExpect(jsonPath("$.orderNumber").value("A26ABC123"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("バリデーション: items 空 → 400 VALIDATION_ERROR")
    void emptyItems() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "items", new Object[]{},
                "customer", Map.of("name", "x", "email", "x@example.com", "deliveryAddress", "x")
        ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("バリデーション: quantity = 0（@Min(1)違反）→ 400")
    void quantityZero() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productId", 1, "quantity", 0)},
                "customer", Map.of("name", "x", "email", "x@example.com", "deliveryAddress", "x")
        ));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("バリデーション: quantity = 4（@Max(3)違反、過剰購入防止）→ 400")
    void quantityOverMax() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productId", 1, "quantity", 4)},
                "customer", Map.of("name", "x", "email", "x@example.com", "deliveryAddress", "x")
        ));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("バリデーション: 不正なメール形式 → 400")
    void invalidEmail() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productId", 1, "quantity", 1)},
                "customer", Map.of("name", "x", "email", "not-an-email", "deliveryAddress", "x")
        ));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("バリデーション: 必須項目欠落 (deliveryAddress 空) → 400")
    void missingRequired() throws Exception {
        String body = om.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productId", 1, "quantity", 1)},
                "customer", Map.of("name", "x", "email", "x@example.com", "deliveryAddress", "")
        ));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("業務エラー: OutOfStockException → 409 OUT_OF_STOCK")
    void outOfStock() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new OutOfStockException());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("セキュリティ: Content-Type 指定なし → 415 UNSUPPORTED_MEDIA_TYPE")
    void missingContentType() throws Exception {
        mockMvc.perform(post("/api/orders").content(validBody()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("セキュリティ: 不正なJSON → 400 MALFORMED_REQUEST (パース失敗が500に化けない)")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
