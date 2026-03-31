package com.ecommerce.search.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.PagedResponse;
import com.ecommerce.search.dto.ProductSearchResult;
import com.ecommerce.search.model.ProductDocument;
import com.ecommerce.search.service.ElasticsearchService;
import com.ecommerce.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Product search APIs powered by Elasticsearch")
public class SearchController {

    private final SearchService searchService;
    private final ElasticsearchService elasticsearchService;

    @GetMapping
    @Operation(summary = "Full-text search across products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSearchResult>>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductSearchResult> results = searchService.search(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/category/{categoryName}")
    @Operation(summary = "Search products by category")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSearchResult>>> searchByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ProductSearchResult> results = searchService.searchByCategory(categoryName, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping("/index")
    @Operation(summary = "Index a product document (internal API)")
    public ResponseEntity<ApiResponse<Void>> indexProduct(@RequestBody ProductDocument product) {
        elasticsearchService.indexProduct(product);
        return ResponseEntity.ok(ApiResponse.success("Product indexed successfully", null));
    }

    @PostMapping("/reindex")
    @Operation(summary = "Bulk reindex products (internal API)")
    public ResponseEntity<ApiResponse<Void>> reindex(@RequestBody List<ProductDocument> products) {
        elasticsearchService.bulkIndex(products);
        return ResponseEntity.ok(ApiResponse.success("Bulk reindex completed for " + products.size() + " products", null));
    }
}
