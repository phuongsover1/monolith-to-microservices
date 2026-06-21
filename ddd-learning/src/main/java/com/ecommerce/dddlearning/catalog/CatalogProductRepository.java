package com.ecommerce.dddlearning.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, Long> {
}
