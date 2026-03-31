package com.ecommerce.product.strategy;

import com.ecommerce.product.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .name("Test Product")
                .basePrice(BigDecimal.valueOf(100.00))
                .currentPrice(BigDecimal.valueOf(100.00))
                .build();
    }

    @Test
    void regularPricingStrategy_returnsBasePrice() {
        PricingStrategy strategy = new RegularPricingStrategy();

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void regularPricingStrategy_noModification() {
        product.setBasePrice(BigDecimal.valueOf(49.99));
        PricingStrategy strategy = new RegularPricingStrategy();

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(49.99));
    }

    @Test
    void flashSalePricingStrategy_appliesDiscount() {
        PricingStrategy strategy = new FlashSalePricingStrategy(BigDecimal.valueOf(30));

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(70.00));
    }

    @Test
    void flashSalePricingStrategy_50percentDiscount() {
        PricingStrategy strategy = new FlashSalePricingStrategy(BigDecimal.valueOf(50));

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void flashSalePricingStrategy_withFractionalPrice() {
        product.setBasePrice(BigDecimal.valueOf(33.33));
        PricingStrategy strategy = new FlashSalePricingStrategy(BigDecimal.valueOf(20));

        BigDecimal price = strategy.calculatePrice(product);

        BigDecimal expected = BigDecimal.valueOf(33.33)
                .subtract(BigDecimal.valueOf(33.33)
                        .multiply(BigDecimal.valueOf(20))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        assertThat(price).isEqualByComparingTo(expected);
    }

    @Test
    void membershipPricingStrategy_goldTier_applies15PercentDiscount() {
        PricingStrategy strategy = new MembershipPricingStrategy("GOLD");

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(85.00));
    }

    @Test
    void membershipPricingStrategy_platinumTier_applies25PercentDiscount() {
        PricingStrategy strategy = new MembershipPricingStrategy("PLATINUM");

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(75.00));
    }

    @Test
    void membershipPricingStrategy_goldCaseInsensitive() {
        PricingStrategy strategy = new MembershipPricingStrategy("gold");

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(85.00));
    }

    @Test
    void membershipPricingStrategy_unknownTier_returnsBasePrice() {
        PricingStrategy strategy = new MembershipPricingStrategy("SILVER");

        BigDecimal price = strategy.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void pricingContext_delegatesToStrategy() {
        PricingStrategy mockStrategy = new FlashSalePricingStrategy(BigDecimal.valueOf(10));
        PricingContext context = new PricingContext(mockStrategy);

        BigDecimal price = context.calculatePrice(product);

        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(90.00));
    }

    @Test
    void pricingContext_canSwitchStrategy() {
        PricingContext context = new PricingContext(new RegularPricingStrategy());

        BigDecimal regularPrice = context.calculatePrice(product);
        assertThat(regularPrice).isEqualByComparingTo(BigDecimal.valueOf(100.00));

        context.setStrategy(new FlashSalePricingStrategy(BigDecimal.valueOf(30)));

        BigDecimal salePrice = context.calculatePrice(product);
        assertThat(salePrice).isEqualByComparingTo(BigDecimal.valueOf(70.00));
    }

    @Test
    void pricingContext_membershipAfterFlashSale() {
        PricingContext context = new PricingContext(new FlashSalePricingStrategy(BigDecimal.valueOf(30)));
        BigDecimal flashPrice = context.calculatePrice(product);
        assertThat(flashPrice).isEqualByComparingTo(BigDecimal.valueOf(70.00));

        context.setStrategy(new MembershipPricingStrategy("PLATINUM"));
        BigDecimal memberPrice = context.calculatePrice(product);
        assertThat(memberPrice).isEqualByComparingTo(BigDecimal.valueOf(75.00));
    }
}
