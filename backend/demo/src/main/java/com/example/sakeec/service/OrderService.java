package com.example.sakeec.service;

import com.example.sakeec.dto.AdminOrderStatusUpdateResponse;
import com.example.sakeec.dto.OrderRequest;
import com.example.sakeec.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);
    List<OrderResponse> findAll(String status);
    AdminOrderStatusUpdateResponse updateStatus(Long id, String status);
}
