package com.ecommerce.product.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .sku(request.sku())
                .brand(request.brand())
                .basePrice(request.basePrice())
                .currentPrice(request.basePrice())
                .build();

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        log.info("Created product: {} ({})", saved.getName(), saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(UUID id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setBrand(request.brand());
        product.setBasePrice(request.basePrice());

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId()));
            product.setCategory(category);
        }

        Product updated = productRepository.save(product);
        log.info("Updated product: {} ({})", updated.getName(), updated.getId());
        return toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(String query, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(query, pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public ProductDto applyPricing(UUID productId, String strategyType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        PricingStrategy strategy = resolvePricingStrategy(strategyType);
        PricingContext context = new PricingContext(strategy);
        BigDecimal newPrice = context.calculatePrice(product);

        product.setCurrentPrice(newPrice);
        Product updated = productRepository.save(product);
        log.info("Applied {} pricing to product {}: {} -> {}", strategyType, productId, product.getBasePrice(), newPrice);
        return toDto(updated);
    }

    private PricingStrategy resolvePricingStrategy(String strategyType) {
        return switch (strategyType.toUpperCase()) {
            case "FLASH_SALE" -> new FlashSalePricingStrategy(BigDecimal.valueOf(30));
            case "MEMBERSHIP_GOLD" -> new MembershipPricingStrategy("GOLD");
            case "MEMBERSHIP_PLATINUM" -> new MembershipPricingStrategy("PLATINUM");
            default -> new RegularPricingStrategy();
        };
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getBrand(),
                product.getBasePrice(),
                product.getCurrentPrice(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrls(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
