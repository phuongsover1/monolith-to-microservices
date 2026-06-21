package com.ecommerce.dddlearning.integration;

import com.ecommerce.dddlearning.catalog.CatalogProduct;
import com.ecommerce.dddlearning.order.Order;

/**
 * Anti-corruption layer at the Catalog → Sales boundary.
 * <p>
 * Sales does not import Catalog's entity graph into its model. At the moment of
 * adding to cart, we <em>translate</em> catalog data into the sales snapshot format.
 */
public final class CatalogToSalesTranslator {

    private CatalogToSalesTranslator() {
    }

    public static void addToOrder(Order order, CatalogProduct product, int quantity) {
        order.addProduct(
                product.getId(),
                product.getName(),
                quantity,
                product.getListPriceCents()
        );
    }
}
