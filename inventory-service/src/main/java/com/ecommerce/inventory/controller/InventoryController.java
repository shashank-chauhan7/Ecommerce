package com.ecommerce.inventory.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.inventory.dto.ReserveStockRequest;
import com.ecommerce.inventory.dto.StockDto;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{region}/{productId}")
    @Operation(summary = "Check stock for a product in a region")
    public ResponseEntity<ApiResponse<StockDto>> getStock(
            @PathVariable String region,
            @PathVariable UUID productId) {
        StockDto stock = inventoryService.getStock(region, productId);
        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @GetMapping("/{region}")
    @Operation(summary = "Get all stock in a region")
    public ResponseEntity<ApiResponse<List<StockDto>>> getStockByRegion(
            @PathVariable String region) {
        List<StockDto> stocks = inventoryService.getStockByRegion(region);
        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @PutMapping("/{region}/{productId}")
    @Operation(summary = "Update stock quantity (admin)")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable String region,
            @PathVariable UUID productId,
            @RequestParam int availableQty) {
        inventoryService.updateStock(region, productId, availableQty);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", null));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock (for testing)")
    public ResponseEntity<ApiResponse<Void>> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        inventoryService.reserveStock(
                request.region(), request.productId(), request.quantity(), request.orderId());
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully", null));
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved stock")
    public ResponseEntity<ApiResponse<Void>> releaseStock(
            @Valid @RequestBody ReserveStockRequest request) {
        inventoryService.releaseStock(
                request.region(), request.productId(), request.quantity(), request.orderId());
        return ResponseEntity.ok(ApiResponse.success("Stock released successfully", null));
    }
}
