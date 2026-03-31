package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.service.UserService;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile and address management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "Get user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String headerUserId) {
        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String headerUserId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto updated = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressDto>> addAddress(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String headerUserId,
            @Valid @RequestBody AddressDto addressDto) {
        AddressDto created = userService.addAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", created));
    }

    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Get all addresses for a user")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAddresses(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String headerUserId) {
        List<AddressDto> addresses = userService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId,
            @RequestHeader("X-User-Id") String headerUserId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }
}
