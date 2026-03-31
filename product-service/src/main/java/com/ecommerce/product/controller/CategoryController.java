package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.CategoryDto;
import com.ecommerce.product.model.Category;
import com.ecommerce.product.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category management APIs")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody CategoryDto request) {
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.parentId()));
            parent.addChild(category);
        }

        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", toDto(saved)));
    }

    @GetMapping
    @Operation(summary = "Get all root categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getRootCategories() {
        List<CategoryDto> roots = categoryRepository.findByParentIsNull()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(roots));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategory(@PathVariable UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return ResponseEntity.ok(ApiResponse.success(toDto(category)));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getChildren(@PathVariable UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        List<CategoryDto> children = category.getChildren()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    private CategoryDto toDto(Category category) {
        List<CategoryDto> childDtos = category.getChildren() != null
                ? category.getChildren().stream().map(this::toDto).toList()
                : List.of();

        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                childDtos
        );
    }
}
