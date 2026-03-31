package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;

public class PricingContext {

    private PricingStrategy strategy;

    public PricingContext(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = strategy;
    }

    public BigDecimal calculatePrice(Product product) {
        return strategy.calculatePrice(product);
    }
}
