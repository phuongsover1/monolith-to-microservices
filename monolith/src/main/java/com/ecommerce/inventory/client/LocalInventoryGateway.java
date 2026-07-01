package com.ecommerce.inventory.client;


import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.service.InventoryService;

@Service
@ConditionalOnProperty(name = "inventory.client.mode", havingValue = "local", matchIfMissing = true)
public class LocalInventoryGateway implements InventoryGateway {

    private static final Logger log = LogManager.getLogger(LocalInventoryGateway.class);
    private final InventoryService inventoryService;

    public LocalInventoryGateway(InventoryService inventoryService) {
        log.info("Initializing LocalInventoryGateway");
        this.inventoryService = inventoryService;
    }

    @Override
    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(inventoryService.getProductEntity(id));
    }

    @Override
    public void reverseStock(Long id, AdjustStockRequest request) {
        inventoryService.reverseStock(id, request);
    }

    @Override
    public void releaseStock(Long id, AdjustStockRequest request) {
        inventoryService.releaseStock(id, request);
    }

    @Override
    public List<ProductResponse> getProducts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProducts'");
    }
    
}
