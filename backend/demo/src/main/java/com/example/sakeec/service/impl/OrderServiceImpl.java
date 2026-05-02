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
import com.example.sakeec.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    // PENDING → SHIPPED or CANCELLED, SHIPPED → DELIVERED, それ以外は遷移不可
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of()
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.Item item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new NotFoundException("product not found: " + item.productId()));

            if (product.getStockQuantity() < item.quantity()) {
                throw new OutOfStockException();
            }

            product.setStockQuantity(product.getStockQuantity() - item.quantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setUnitPrice(product.getPrice());
            items.add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName(request.customer().name());
        order.setCustomerEmail(request.customer().email());
        order.setCustomerPhone(request.customer().phone());
        order.setDeliveryAddress(request.customer().deliveryAddress());
        order.setTotalAmount(total);
        order.setStatus("PENDING");
        Order saved = orderRepository.save(order);

        items.forEach(item -> item.setOrder(saved));
        orderItemRepository.saveAll(items);

        log.info("ORDER_CREATED orderNumber={} total={}", saved.getOrderNumber(), saved.getTotalAmount());
        return toResponse(saved);
    }

    @Override
    public List<OrderResponse> findAll(String status) {
        List<Order> orders = (status != null && !status.isBlank())
                ? orderRepository.findByStatus(status)
                : orderRepository.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AdminOrderStatusUpdateResponse updateStatus(Long id, String newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("order not found"));

        String current = order.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new InvalidStatusTransitionException(current, newStatus, current);
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("ORDER_STATUS_CHANGED orderId={} from={} to={}", id, current, newStatus);
        return new AdminOrderStatusUpdateResponse(saved.getId(), saved.getStatus(), Instant.now());
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getTotalAmount(), o.getStatus());
    }

    private String generateOrderNumber() {
        String year = String.format("%02d", Year.now().getValue() % 100);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "A" + year + random;
    }
}
