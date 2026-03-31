package com.ecommerce.user.service;

import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileDto;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserProfileDto getProfile(UUID userId);

    UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request);

    AddressDto addAddress(UUID userId, AddressDto addressDto);

    List<AddressDto> getAddresses(UUID userId);

    void deleteAddress(UUID userId, UUID addressId);
}
