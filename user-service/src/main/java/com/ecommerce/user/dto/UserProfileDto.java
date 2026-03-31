package com.ecommerce.user.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        List<AddressDto> addresses,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
