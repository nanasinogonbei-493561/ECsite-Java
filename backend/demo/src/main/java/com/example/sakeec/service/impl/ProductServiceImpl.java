package com.example.sakeec.service.impl;

import com.example.sakeec.dto.AdminProductRequest;
import com.example.sakeec.dto.ProductResponse;
import com.example.sakeec.entity.Product;
import com.example.sakeec.exception.NotFoundException;
import com.example.sakeec.repository.ProductRepository;
import com.example.sakeec.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductResponse> findAll(String query, int limit) {
        int safeLimit = Math.min(limit, 30);
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<Product> products = (query != null && !query.isBlank())
                ? productRepository.findByNameContainingIgnoreCase(query, pageable)
                : productRepository.findAll(pageable).getContent();
        return products.stream().map(this::toResponse).toList();
    }

    @Override
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("product not found"));
    }

    @Override
    @Transactional
    public ProductResponse create(AdminProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setImagePath(request.imageUrl());
        product.setStockQuantity(0);
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, AdminProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("product not found"));
        product.setName(request.name());
        product.setPrice(request.price());
        product.setImagePath(request.imageUrl());
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("product not found");
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getBrewery(),
                p.getPrice(),
                p.getVolume(),
                p.getAlcoholContent(),
                p.getDescription(),
                p.getImagePath(),
                p.getStockQuantity()
        );
    }
}
