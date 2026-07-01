package com.ecommerce.order.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.client.InventoryGateway;
import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.user.domain.User;
import com.ecommerce.user.service.UserService;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final InventoryGateway inventoryGateway;

    public OrderService(
            OrderRepository orderRepository,
            UserService userService,
            InventoryGateway inventoryGateway) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.inventoryGateway = inventoryGateway;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        userService.getUserEntity(userId);
        List<Order> orders = orderRepository.findByUserIdWithDetails(userId);

        // Create Set to lookup all product in orders
        Set<Long> productIdSet = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(orderItem -> orderItem.getProductId())
                .collect(Collectors.toSet());
        List<ProductResponse> products = productIdSet.stream()
                .map(id -> inventoryGateway.getProduct(id))
                .toList();
        return orders.stream().map(order -> buildOrderResponse(order, products))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        // Fetch all products at once
        List<ProductResponse> products = order.getItems().stream()
                .map(item -> inventoryGateway.getProduct(item.getProductId()))
                .toList();

        return buildOrderResponse(order, products);
    }

    private OrderResponse buildOrderResponse(Order order, List<ProductResponse> products) {
        Map<Long, ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(ProductResponse::id, p -> p));

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.from(item, productMap.get(item.getProductId())))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotalCents(),
                order.getCreatedAt(),
                items);
    }

    public OrderResponse create(CreateOrderRequest request) {
        User user = userService.getUserEntity(request.userId());
        Order order = new Order(user);

        for (CreateOrderRequest.OrderLineRequest line : request.items()) {
            ProductResponse product = inventoryGateway.getProduct(line.productId());
            inventoryGateway.reverseStock(product.id(), new AdjustStockRequest(line.quantity()));
            order.addItem(new OrderItem(product.id(), line.quantity(), product.priceCents()));
        }

        order.confirm();
        Order saved = orderRepository.save(order);

        List<ProductResponse> products = getProductsFromOrderItems(saved.getItems());
        return buildOrderResponse(saved, products);
    }

    public OrderResponse cancel(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (order.getStatus() == com.ecommerce.order.domain.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        for (OrderItem item : order.getItems()) {
            inventoryGateway.releaseStock(item.getProductId(), new AdjustStockRequest(item.getQuantity()));
        }

        order.cancel();

        List<ProductResponse> products = getProductsFromOrderItems(order.getItems());

        return buildOrderResponse(order, products);
    }

    private List<ProductResponse> getProductsFromOrderItems(List<OrderItem> orderItems) {
        List<ProductResponse> products = orderItems.stream()
                .map(item -> inventoryGateway.getProduct(item.getProductId()))
                .toList();
        return products;
    }
}
