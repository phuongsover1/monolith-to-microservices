package com.ecommerce.inventory.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.inventory.dto.AdjustStockRequest;
import com.ecommerce.inventory.dto.CreateProductRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory/products")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<ProductResponse> listProducts() {
        return inventoryService.findAll();
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return inventoryService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/{id}/reverse-stock")
    public ProductResponse reverseStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.reverseStock(id, request);
    }

    @PutMapping("/{id}/release-stock")
    public ProductResponse releaseStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return inventoryService.releaseStock(id, request);
    }
}
