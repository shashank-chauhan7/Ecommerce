package com.ecommerce.product.service.decorator;

import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Primary
@Slf4j
public class CachingProductServiceDecorator implements ProductService {

    private final ProductService delegate;

    public CachingProductServiceDecorator(@Qualifier("productServiceImpl") ProductService delegate) {
        this.delegate = delegate;
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ProductDto createProduct(CreateProductRequest request) {
        log.debug("Cache evict: creating product");
        return delegate.createProduct(request);
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public ProductDto updateProduct(UUID id, CreateProductRequest request) {
        log.debug("Cache evict: updating product {}", id);
        return delegate.updateProduct(id, request);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductDto getProductById(UUID id) {
        log.debug("Cache miss: loading product {}", id);
        return delegate.getProductById(id);
    }

    @Override
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return delegate.getAllProducts(pageable);
    }

    @Override
    public Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return delegate.getProductsByCategory(categoryId, pageable);
    }

    @Override
    public Page<ProductDto> searchProducts(String query, Pageable pageable) {
        return delegate.searchProducts(query, pageable);
    }

    @Override
    @CacheEvict(value = "products", key = "#productId")
    public ProductDto applyPricing(UUID productId, String strategyType) {
        log.debug("Cache evict: applying pricing to product {}", productId);
        return delegate.applyPricing(productId, strategyType);
    }
}
