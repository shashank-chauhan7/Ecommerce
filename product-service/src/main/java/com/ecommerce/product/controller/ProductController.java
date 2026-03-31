package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ImageService;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management APIs")
public class ProductController {

    private final ProductService productService;
    private final ImageService imageService;

    @PostMapping
    @Operation(summary = "Create a new product (ADMIN only)")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @RequestHeader("X-User-Roles") String roles) {
        validateAdminRole(roles);
        ProductDto product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto updated = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping
    @Operation(summary = "Get all products (paginated)")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getAllProducts(Pageable pageable) {
        Page<ProductDto> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> getProductsByCategory(
            @PathVariable UUID categoryId, Pageable pageable) {
        Page<ProductDto> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<ApiResponse<Page<ProductDto>>> searchProducts(
            @RequestParam String query, Pageable pageable) {
        Page<ProductDto> products = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image")
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = imageService.uploadImage(file, id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Image uploaded successfully", imageUrl));
    }

    @PostMapping("/{id}/pricing")
    @Operation(summary = "Apply pricing strategy to product")
    public ResponseEntity<ApiResponse<ProductDto>> applyPricing(
            @PathVariable UUID id,
            @RequestParam String strategy) {
        ProductDto updated = productService.applyPricing(id, strategy);
        return ResponseEntity.ok(ApiResponse.success("Pricing applied successfully", updated));
    }

    private void validateAdminRole(String roles) {
        if (roles == null || !roles.toUpperCase().contains("ADMIN")) {
            throw new SecurityException("Access denied: ADMIN role required");
        }
    }
}
