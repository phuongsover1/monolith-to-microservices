/**
 * <b>Sales bounded context</b> — together with {@code com.ecommerce.dddlearning.customer}.
 * <p>
 * An {@link Order} stores a <em>snapshot</em> of product name and price at the moment
 * the buyer adds items. That snapshot is independent from the live
 * {@link com.ecommerce.dddlearning.catalog.CatalogProduct} in the Catalog context.
 */
package com.ecommerce.dddlearning.order;
