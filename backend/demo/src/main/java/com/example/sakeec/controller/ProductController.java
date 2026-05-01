package com.example.sakeec.controller;

import com.example.sakeec.dto.ProductResponse;
import com.example.sakeec.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(productService.findAll(q, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
