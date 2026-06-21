package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.domain.Product;
import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.CreateProductRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("SKU already exists: " + request.sku());
        }
        Product saved = productRepository.save(new Product(
                request.sku(),
                request.name(),
                request.description(),
                request.priceCents(),
                request.stockQty()
        ));
        return ProductResponse.from(saved);
    }

    public ProductResponse restock(Long id, AdjustStockRequest request) {
        Product product = getProductEntity(id);
        product.releaseStock(request.quantity());
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public void reserveStock(Long productId, int quantity) {
        Product product = getProductEntity(productId);
        product.reserveStock(quantity);
    }

    public void releaseStock(Long productId, int quantity) {
        Product product = getProductEntity(productId);
        product.releaseStock(quantity);
    }
}
