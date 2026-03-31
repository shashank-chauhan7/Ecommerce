package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;

public class RegularPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Product product) {
        return product.getBasePrice();
    }
}
