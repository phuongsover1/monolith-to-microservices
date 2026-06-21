/**
 * <b>Sales bounded context</b> — together with {@code com.ecommerce.dddlearning.order}.
 * <p>
 * Ubiquitous language here: <em>Customer</em> (a registered buyer), <em>Order</em>,
 * <em>place an order</em>, <em>line item</em> with a frozen unit price.
 * <p>
 * A Customer is <strong>not</strong> the same thing as a
 * {@link com.ecommerce.dddlearning.shipping.DeliveryRecipient} in the Shipping context,
 * even when they represent the same person in the real world.
 */
package com.ecommerce.dddlearning.customer;
