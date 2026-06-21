package com.ecommerce.dddlearning;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.dddlearning.catalog.CatalogProduct;
import com.ecommerce.dddlearning.catalog.CatalogProductRepository;
import com.ecommerce.dddlearning.customer.Customer;
import com.ecommerce.dddlearning.customer.CustomerRepository;
import com.ecommerce.dddlearning.integration.CatalogToSalesTranslator;
import com.ecommerce.dddlearning.integration.SalesToShippingTranslator;
import com.ecommerce.dddlearning.order.Order;
import com.ecommerce.dddlearning.order.OrderRepository;
import com.ecommerce.dddlearning.order.OrderStatus;
import com.ecommerce.dddlearning.shipping.DeliveryRecipient;
import com.ecommerce.dddlearning.shipping.Shipment;
import com.ecommerce.dddlearning.shipping.ShipmentRepository;
import com.ecommerce.dddlearning.shipping.ShippingAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * End-to-end across three bounded contexts in one monolith database —
 * each context still owns its own tables, entities, and language.
 */
@DataJpaTest
class BoundedContextIntegrationTest {

    @Autowired
    private CatalogProductRepository catalogProductRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Placing an order translates Catalog → Sales → Shipping without sharing entity graphs")
    void placeOrderAndScheduleShipmentAcrossContexts() {
        CatalogProduct book = catalogProductRepository.save(new CatalogProduct(
                "Monolith to Microservices",
                "Decomposition patterns",
                "Books",
                3999
        ));

        Customer buyer = customerRepository.save(new Customer("Sam Newman"));
        Order order = new Order(buyer.id());
        CatalogToSalesTranslator.addToOrder(order, book, 1);
        order.place();
        Order savedOrder = orderRepository.save(order);

        book.updateListPrice(4999);
        catalogProductRepository.save(book);

        var recipient = new DeliveryRecipient("Warehouse dock contact", "+1-555-0200");
        var address = new ShippingAddress("100 Fulfilment Way", "London", "E1 6AN", "UK");
        Shipment shipment = SalesToShippingTranslator.scheduleShipment(savedOrder, recipient, address);
        shipmentRepository.save(shipment);

        entityManager.flush();
        entityManager.clear();

        Order reloadedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        Shipment reloadedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        CatalogProduct reloadedBook = catalogProductRepository.findById(book.getId()).orElseThrow();

        assertThat(reloadedBook.getListPriceCents()).isEqualTo(4999);
        assertThat(reloadedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(reloadedOrder.totalCents()).isEqualTo(3999);
        assertThat(reloadedShipment.getOrderId()).isEqualTo(savedOrder.getId());
        assertThat(reloadedShipment.getRecipient().name()).isEqualTo("Warehouse dock contact");
        assertThat(reloadedShipment.lines()).hasSize(1);
    }
}
