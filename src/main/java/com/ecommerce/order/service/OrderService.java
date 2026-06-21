package com.ecommerce.order.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.domain.Product;
import com.ecommerce.inventory.service.InventoryService;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.user.domain.User;
import com.ecommerce.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    public OrderService(
            OrderRepository orderRepository,
            UserService userService,
            InventoryService inventoryService
    ) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        userService.getUserEntity(userId);
        return orderRepository.findByUserIdWithDetails(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public OrderResponse create(CreateOrderRequest request) {
        User user = userService.getUserEntity(request.userId());
        Order order = new Order(user);

        for (CreateOrderRequest.OrderLineRequest line : request.items()) {
            Product product = inventoryService.getProductEntity(line.productId());
            inventoryService.reserveStock(product.getId(), line.quantity());
            order.addItem(new OrderItem(product, line.quantity()));
        }

        order.confirm();
        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved);
    }

    public OrderResponse cancel(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (order.getStatus() == com.ecommerce.order.domain.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order.cancel();
        return OrderResponse.from(order);
    }
}
