package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePrice(Product product);
}
