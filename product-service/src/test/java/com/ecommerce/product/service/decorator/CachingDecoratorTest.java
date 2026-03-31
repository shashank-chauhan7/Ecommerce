package com.ecommerce.product.service.decorator;

import com.ecommerce.product.dto.CreateProductRequest;
import com.ecommerce.product.dto.ProductDto;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingDecoratorTest {

    @Mock
    private ProductService delegate;

    private CachingProductServiceDecorator decorator;

    private UUID productId;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        decorator = new CachingProductServiceDecorator(delegate);

        productId = UUID.randomUUID();
        testProductDto = new ProductDto(
                productId, "Test Product", "Description", "SKU-001", "Brand",
                BigDecimal.valueOf(99.99), BigDecimal.valueOf(99.99),
                null, null, List.of(), true, Instant.now(), Instant.now()
        );
    }

    @Test
    void getProductById_delegatesToUnderlying() {
        when(delegate.getProductById(productId)).thenReturn(testProductDto);

        ProductDto result = decorator.getProductById(productId);

        assertThat(result).isEqualTo(testProductDto);
        verify(delegate).getProductById(productId);
    }

    @Test
    void createProduct_delegatesToUnderlying() {
        CreateProductRequest request = new CreateProductRequest(
                "New Product", "Desc", "SKU-002", "Brand", BigDecimal.TEN, null
        );
        when(delegate.createProduct(request)).thenReturn(testProductDto);

        ProductDto result = decorator.createProduct(request);

        assertThat(result).isEqualTo(testProductDto);
        verify(delegate).createProduct(request);
    }

    @Test
    void updateProduct_delegatesToUnderlying() {
        CreateProductRequest request = new CreateProductRequest(
                "Updated", "Desc", "SKU-003", "Brand", BigDecimal.TEN, null
        );
        when(delegate.updateProduct(productId, request)).thenReturn(testProductDto);

        ProductDto result = decorator.updateProduct(productId, request);

        assertThat(result).isEqualTo(testProductDto);
        verify(delegate).updateProduct(productId, request);
    }

    @Test
    void getAllProducts_delegatesToUnderlying() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductDto> page = new PageImpl<>(List.of(testProductDto));
        when(delegate.getAllProducts(pageable)).thenReturn(page);

        Page<ProductDto> result = decorator.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(delegate).getAllProducts(pageable);
    }

    @Test
    void searchProducts_delegatesToUnderlying() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductDto> page = new PageImpl<>(List.of(testProductDto));
        when(delegate.searchProducts("mouse", pageable)).thenReturn(page);

        Page<ProductDto> result = decorator.searchProducts("mouse", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(delegate).searchProducts("mouse", pageable);
    }

    @Test
    void applyPricing_delegatesToUnderlying() {
        when(delegate.applyPricing(productId, "FLASH_SALE")).thenReturn(testProductDto);

        ProductDto result = decorator.applyPricing(productId, "FLASH_SALE");

        assertThat(result).isEqualTo(testProductDto);
        verify(delegate).applyPricing(productId, "FLASH_SALE");
    }

    @Test
    void getProductsByCategory_delegatesToUnderlying() {
        UUID categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductDto> page = new PageImpl<>(List.of(testProductDto));
        when(delegate.getProductsByCategory(categoryId, pageable)).thenReturn(page);

        Page<ProductDto> result = decorator.getProductsByCategory(categoryId, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(delegate).getProductsByCategory(categoryId, pageable);
    }

    @Test
    void multipleGetProductByIdCalls_allDelegateToUnderlying() {
        when(delegate.getProductById(productId)).thenReturn(testProductDto);

        decorator.getProductById(productId);
        decorator.getProductById(productId);

        verify(delegate, times(2)).getProductById(productId);
    }
}
