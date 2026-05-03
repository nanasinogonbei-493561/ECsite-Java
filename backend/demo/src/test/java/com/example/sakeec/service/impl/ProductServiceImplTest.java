package com.example.sakeec.service.impl;

import com.example.sakeec.dto.AdminProductRequest;
import com.example.sakeec.entity.Product;
import com.example.sakeec.exception.NotFoundException;
import com.example.sakeec.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductServiceImpl service;

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("test");
        p.setPrice(new BigDecimal("1000"));
        p.setStockQuantity(10);
        return p;
    }

    @Test
    @DisplayName("運用: limit が 30 を超えても 30 にクランプされ、DoS的な大量取得を防ぐ")
    void limitClamp() {
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        service.findAll(null, 9999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(30);
    }

    @Test
    @DisplayName("limit が 30 以下ならその値が使われる")
    void limitUnderCap() {
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        service.findAll(null, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("query 指定時は名前部分一致検索が呼ばれる")
    void searchByQuery() {
        when(productRepository.findByNameContainingIgnoreCase(eq("獺祭"), any(Pageable.class)))
                .thenReturn(List.of(product(1L)));

        var res = service.findAll("獺祭", 30);

        assertThat(res).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCase(eq("獺祭"), any(Pageable.class));
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("query が空白のみのとき: 検索ではなく全件取得にフォールバック")
    void blankQueryFallsBack() {
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        service.findAll("   ", 30);

        verify(productRepository).findAll(any(Pageable.class));
        verify(productRepository, never())
                .findByNameContainingIgnoreCase(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("findById 存在しない場合 NotFoundException")
    void findByIdNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("delete 存在しない場合 NotFoundException、deleteById は呼ばれない")
    void deleteNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("create: stockQuantity 未指定 (null) なら 0 で保存される")
    void createWithoutStockDefaultsToZero() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var res = service.create(new AdminProductRequest("酒A", new BigDecimal("1500"), "/img/a.jpg", null));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getStockQuantity()).isEqualTo(0);
        assertThat(res.stockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("create: stockQuantity 指定時はその値で保存される")
    void createWithStock() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var res = service.create(new AdminProductRequest("酒A", new BigDecimal("1500"), "/img/a.jpg", 12));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getStockQuantity()).isEqualTo(12);
        assertThat(res.stockQuantity()).isEqualTo(12);
    }
}
