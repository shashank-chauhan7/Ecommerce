package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FlashSalePricingStrategy implements PricingStrategy {

    private final BigDecimal discountPercentage;

    public FlashSalePricingStrategy(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    @Override
    public BigDecimal calculatePrice(Product product) {
        BigDecimal discount = product.getBasePrice()
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return product.getBasePrice().subtract(discount);
    }
}
