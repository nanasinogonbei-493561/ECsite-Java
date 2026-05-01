package com.example.sakeec.service.impl;

import com.example.sakeec.dto.AdminOrderStatusUpdateResponse;
import com.example.sakeec.dto.OrderRequest;
import com.example.sakeec.dto.OrderResponse;
import com.example.sakeec.entity.Order;
import com.example.sakeec.entity.OrderItem;
import com.example.sakeec.entity.Product;
import com.example.sakeec.exception.InvalidStatusTransitionException;
import com.example.sakeec.exception.NotFoundException;
import com.example.sakeec.exception.OutOfStockException;
import com.example.sakeec.repository.OrderItemRepository;
import com.example.sakeec.repository.OrderRepository;
import com.example.sakeec.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private OrderServiceImpl service;

    @Captor private ArgumentCaptor<List<OrderItem>> orderItemsCaptor;

    private Product product(long id, String name, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        return p;
    }

    private OrderRequest singleItemRequest(long productId, int quantity) {
        return new OrderRequest(
                List.of(new OrderRequest.Item(productId, quantity)),
                new OrderRequest.Customer("田中太郎", "tanaka@example.com", "090-0000-0000", "東京都渋谷区")
        );
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("正常系: 在庫減算・小計計算・PENDINGで保存・orderNumber形式")
        void createsOrderHappyPath() {
            Product p = product(1L, "獺祭", "3000.00", 10);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderResponse res = service.createOrder(singleItemRequest(1L, 2));

            assertThat(p.getStockQuantity()).isEqualTo(8);
            assertThat(res.totalAmount()).isEqualByComparingTo("6000.00");
            assertThat(res.status()).isEqualTo("PENDING");
            // A{2桁年}{6桁HEX大文字}
            assertThat(res.orderNumber()).matches("^A\\d{2}[0-9A-F]{6}$");

            verify(productRepository).save(p);
            verify(orderRepository).save(any(Order.class));
            verify(orderItemRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("商品が存在しない: NotFoundExceptionで在庫操作も保存も発生しない")
        void productNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrder(singleItemRequest(99L, 1)))
                    .isInstanceOf(NotFoundException.class);

            verify(productRepository, never()).save(any());
            verify(orderRepository, never()).save(any());
            verify(orderItemRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("在庫不足: OutOfStockException、在庫は変動せず注文も保存されない")
        void outOfStock() {
            Product p = product(1L, "獺祭", "3000.00", 1);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.createOrder(singleItemRequest(1L, 3)))
                    .isInstanceOf(OutOfStockException.class);

            assertThat(p.getStockQuantity()).isEqualTo(1); // 変動なし
            verify(productRepository, never()).save(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("複数商品: 個別に在庫減算され、totalAmountが合算される")
        void multipleItems() {
            Product p1 = product(1L, "獺祭", "3000.00", 5);
            Product p2 = product(2L, "久保田", "5000.00", 5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(productRepository.findById(2L)).thenReturn(Optional.of(p2));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderRequest req = new OrderRequest(
                    List.of(
                            new OrderRequest.Item(1L, 2),
                            new OrderRequest.Item(2L, 1)
                    ),
                    new OrderRequest.Customer("佐藤", "sato@example.com", null, "大阪府")
            );

            OrderResponse res = service.createOrder(req);

            assertThat(p1.getStockQuantity()).isEqualTo(3);
            assertThat(p2.getStockQuantity()).isEqualTo(4);
            // 3000*2 + 5000*1 = 11000
            assertThat(res.totalAmount()).isEqualByComparingTo("11000.00");

            // OrderItem を 2 件 saveAll
            verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
            assertThat(orderItemsCaptor.getValue()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("updateStatus: ステータス遷移マシン")
    class UpdateStatus {

        @ParameterizedTest(name = "{0} → {1} は許可")
        @CsvSource({
                "PENDING,SHIPPED",
                "PENDING,CANCELLED",
                "SHIPPED,DELIVERED"
        })
        void allowedTransitions(String from, String to) {
            Order o = new Order();
            o.setId(10L);
            o.setStatus(from);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(o));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            AdminOrderStatusUpdateResponse res = service.updateStatus(10L, to);

            assertThat(res.status()).isEqualTo(to);
            assertThat(o.getStatus()).isEqualTo(to);
        }

        @ParameterizedTest(name = "{0} → {1} は不許可")
        @CsvSource({
                "PENDING,DELIVERED",   // SHIPPEDをスキップ
                "PENDING,PENDING",     // 同一遷移
                "SHIPPED,CANCELLED",   // 出荷後キャンセル不可
                "SHIPPED,PENDING",     // 巻き戻し不可
                "DELIVERED,SHIPPED",   // 完了後の変更不可
                "DELIVERED,CANCELLED",
                "CANCELLED,PENDING",
                "CANCELLED,SHIPPED"
        })
        void disallowedTransitions(String from, String to) {
            Order o = new Order();
            o.setId(10L);
            o.setStatus(from);
            when(orderRepository.findById(10L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.updateStatus(10L, to))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasFieldOrPropertyWithValue("currentStatus", from);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("注文が存在しない場合 NotFoundException")
        void notFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(99L, "SHIPPED"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("未知のステータスを指定された場合も InvalidStatusTransitionException")
        void unknownStatus() {
            Order o = new Order();
            o.setId(10L);
            o.setStatus("PENDING");
            when(orderRepository.findById(10L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> service.updateStatus(10L, "WHATEVER"))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {
        @Test
        @DisplayName("status 指定なし: 作成日時降順で全件取得")
        void all() {
            when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
            service.findAll(null);
            verify(orderRepository, times(1)).findAllByOrderByCreatedAtDesc();
            verify(orderRepository, never()).findByStatus(any());
        }

        @Test
        @DisplayName("status 指定あり: 該当ステータスのみ取得")
        void byStatus() {
            when(orderRepository.findByStatus("SHIPPED")).thenReturn(List.of());
            service.findAll("SHIPPED");
            verify(orderRepository).findByStatus("SHIPPED");
            verify(orderRepository, never()).findAllByOrderByCreatedAtDesc();
        }
    }
}
