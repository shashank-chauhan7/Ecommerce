package com.ecommerce.user.dto;

import java.io.Serializable;
import java.util.UUID;

public record AddressDto(
        UUID id,
        String street,
        String city,
        String state,
        String zipCode,
        String country,
        boolean isDefault
) implements Serializable {
}
