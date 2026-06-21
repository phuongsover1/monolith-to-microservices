/**
 * <b>Shipping bounded context</b>.
 * <p>
 * Ubiquitous language here: <em>Shipment</em>, <em>recipient</em>, <em>delivery address</em>,
 * <em>dispatch</em>, <em>track</em>.
 * <p>
 * Shipping does not know about {@link com.ecommerce.dddlearning.customer.Customer}.
 * It only needs to know <em>where</em> and <em>to whom</em> to deliver, plus an
 * {@code orderId} reference back to Sales.
 */
package com.ecommerce.dddlearning.shipping;
