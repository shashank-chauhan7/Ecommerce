package com.ecommerce.product.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        testProduct = Product.builder()
                .id(productId)
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .sku("WM-001")
                .brand("TechBrand")
                .basePrice(BigDecimal.valueOf(29.99))
                .currentPrice(BigDecimal.valueOf(29.99))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest(
                "Wireless Mouse", "Ergonomic wireless mouse", "WM-001", "TechBrand",
                BigDecimal.valueOf(29.99), null
        );

        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDto result = productService.createProduct(request);

        assertThat(result.name()).isEqualTo("Wireless Mouse");
        assertThat(result.basePrice()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        assertThat(result.active()).isTrue();

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_withCategory() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Electronics").build();
        testProduct.setCategory(category);

        CreateProductRequest request = new CreateProductRequest(
                "Wireless Mouse", "Ergonomic wireless mouse", "WM-001", "TechBrand",
                BigDecimal.valueOf(29.99), categoryId
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDto result = productService.createProduct(request);

        assertThat(result.categoryId()).isEqualTo(categoryId);
        assertThat(result.categoryName()).isEqualTo("Electronics");
    }

    @Test
    void getProductById_found() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        ProductDto result = productService.getProductById(productId);

        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.name()).isEqualTo("Wireless Mouse");
        assertThat(result.sku()).isEqualTo("WM-001");
    }

    @Test
    void getProductById_notFound_throwsResourceNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(missingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyPricing_flashSale_priceReducedBy30Percent() {
        testProduct.setBasePrice(BigDecimal.valueOf(100.00));
        testProduct.setCurrentPrice(BigDecimal.valueOf(100.00));

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        Product pricedProduct = Product.builder()
                .id(productId)
                .name("Wireless Mouse")
                .basePrice(BigDecimal.valueOf(100.00))
                .currentPrice(BigDecimal.valueOf(70.00))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(pricedProduct);

        ProductDto result = productService.applyPricing(productId, "FLASH_SALE");

        assertThat(result.currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(70.00));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void applyPricing_membershipGold_priceReducedBy15Percent() {
        testProduct.setBasePrice(BigDecimal.valueOf(100.00));
        testProduct.setCurrentPrice(BigDecimal.valueOf(100.00));

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        Product pricedProduct = Product.builder()
                .id(productId)
                .name("Wireless Mouse")
                .basePrice(BigDecimal.valueOf(100.00))
                .currentPrice(BigDecimal.valueOf(85.00))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(pricedProduct);

        ProductDto result = productService.applyPricing(productId, "MEMBERSHIP_GOLD");

        assertThat(result.currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(85.00));
    }

    @Test
    void applyPricing_regular_keepsBasePrice() {
        testProduct.setBasePrice(BigDecimal.valueOf(100.00));
        testProduct.setCurrentPrice(BigDecimal.valueOf(100.00));

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDto result = productService.applyPricing(productId, "REGULAR");

        assertThat(result.currentPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void updateProduct_success() {
        CreateProductRequest request = new CreateProductRequest(
                "Updated Mouse", "Updated description", "WM-002", "NewBrand",
                BigDecimal.valueOf(39.99), null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        Product updatedProduct = Product.builder()
                .id(productId)
                .name("Updated Mouse")
                .description("Updated description")
                .sku("WM-002")
                .brand("NewBrand")
                .basePrice(BigDecimal.valueOf(39.99))
                .currentPrice(BigDecimal.valueOf(39.99))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        ProductDto result = productService.updateProduct(productId, request);

        assertThat(result.name()).isEqualTo("Updated Mouse");
        assertThat(result.brand()).isEqualTo("NewBrand");
    }
}
