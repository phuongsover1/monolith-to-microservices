package com.ecommerce.inventory.client;

import java.util.List;

import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.ProductResponse;

public interface InventoryGateway {
    ProductResponse getProduct(Long id);

    List<ProductResponse> getProducts();

    void reverseStock(Long id, AdjustStockRequest request);

    void releaseStock(Long id, AdjustStockRequest request);

}
