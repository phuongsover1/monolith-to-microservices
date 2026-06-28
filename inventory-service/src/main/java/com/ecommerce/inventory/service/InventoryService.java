package com.ecommerce.inventory.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.repository.ProductRepository;


@Service
@Transactional
public class InventoryService {
    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = false)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = false)
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
        .map(ProductResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    
}
