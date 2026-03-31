package com.ecommerce.product.service;

import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductDto createProduct(CreateProductRequest request);

    ProductDto updateProduct(UUID id, CreateProductRequest request);

    ProductDto getProductById(UUID id);

    Page<ProductDto> getAllProducts(Pageable pageable);

    Page<ProductDto> getProductsByCategory(UUID categoryId, Pageable pageable);

    Page<ProductDto> searchProducts(String query, Pageable pageable);

    ProductDto applyPricing(UUID productId, String strategyType);
}
