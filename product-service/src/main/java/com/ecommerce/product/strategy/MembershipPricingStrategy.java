package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class MembershipPricingStrategy implements PricingStrategy {

    private static final Map<String, BigDecimal> TIER_DISCOUNTS = Map.of(
            "GOLD", BigDecimal.valueOf(15),
            "PLATINUM", BigDecimal.valueOf(25)
    );

    private final String tier;

    public MembershipPricingStrategy(String tier) {
        this.tier = tier.toUpperCase();
    }

    @Override
    public BigDecimal calculatePrice(Product product) {
        BigDecimal discountPercentage = TIER_DISCOUNTS.getOrDefault(tier, BigDecimal.ZERO);
        BigDecimal discount = product.getBasePrice()
                .multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return product.getBasePrice().subtract(discount);
    }
}
