package com.ecommerce.dddlearning.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.dddlearning.customer.Customer;
import com.ecommerce.dddlearning.customer.CustomerId;
import com.ecommerce.dddlearning.customer.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Shows how persistence respects aggregate boundaries:
 * load/save always goes through OrderRepository (the root), never through
 * a separate line-item repository.
 */
@DataJpaTest
class OrderAggregatePersistenceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Persisting an order saves the whole aggregate graph in one transaction")
    void persistAndReloadWholeAggregate() {
        Customer customer = customerRepository.save(new Customer("Sam Newman"));
        CustomerId customerId = customer.id();

        Order order = new Order(customerId);
        order.addProduct(101L, "Monolith to Microservices", 1, 3999);
        order.addProduct(102L, "Building Microservices", 2, 4999);
        order.place();

        Order saved = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getCustomerId()).isEqualTo(customerId);
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(reloaded.lineItems()).hasSize(2);
        assertThat(reloaded.totalCents()).isEqualTo(3999 + (2 * 4999));
        // Customer is NOT eagerly loaded as part of the order graph.
        assertThat(reloaded.getCustomerId().value()).isEqualTo(customer.getId());
    }
}
