package com.example.sakeec.controller;

import com.example.sakeec.dto.AdminOrderStatusUpdateRequest;
import com.example.sakeec.dto.AdminOrderStatusUpdateResponse;
import com.example.sakeec.dto.OrderResponse;
import com.example.sakeec.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.findAll(status));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AdminOrderStatusUpdateResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody @Valid AdminOrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.status()));
    }
}
