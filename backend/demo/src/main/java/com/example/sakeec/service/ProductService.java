package com.example.sakeec.service;

import com.example.sakeec.dto.AdminProductRequest;
import com.example.sakeec.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> findAll(String query, int limit);
    ProductResponse findById(Long id);
    ProductResponse create(AdminProductRequest request);
    ProductResponse update(Long id, AdminProductRequest request);
    void delete(Long id);
}
