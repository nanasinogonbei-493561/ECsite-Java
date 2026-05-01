package com.example.sakeec.controller;

import com.example.sakeec.dto.OrderRequest;
import com.example.sakeec.dto.OrderResponse;
import com.example.sakeec.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderNumber}")
                .buildAndExpand(response.orderNumber())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
