package com.ecommerce.dddlearning.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Only the aggregate root has a repository.
 * There is intentionally no OrderLineItemRepository.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
