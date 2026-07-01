package com.ecommerce.inventory.client;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.ProductResponse;

@Service
@ConditionalOnProperty(name = "inventory.client.mode", havingValue = "remote")
public class RemoteInventoryClient implements InventoryGateway {
    private static final Logger log = LogManager.getLogger(RemoteInventoryClient.class);
    private final RestClient restClient;

    public RemoteInventoryClient(RestClient.Builder builder,
            @Value("${inventory.client.base-url}") String baseUrl) {
        log.info("Initializing RemoteInventoryClient");
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ProductResponse getProduct(Long id) {
        return restClient.get()
                .uri("/api/inventory/products/{id}", id)
                .retrieve()
                .body(ProductResponse.class);
    }

    public List<ProductResponse> getProducts() {
        return restClient.get()
                .uri("/api/inventory/products")
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductResponse>>() {
                });
    }

    @Override
    public void reverseStock(Long id, AdjustStockRequest request) {
        restClient.put()
                .uri("/api/inventory/products/{id}/reverse-stock", id)
                .body(request)
                .retrieve()
                .body(Void.class);
    }

    @Override
    public void releaseStock(Long id, AdjustStockRequest request) {
        restClient.put()
                .uri("/api/inventory/products/{id}/release-stock", id)
                .body(request)
                .retrieve()
                .body(Void.class);
    }

}
