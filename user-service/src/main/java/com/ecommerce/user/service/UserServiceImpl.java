package com.ecommerce.user.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.user.dto.AddressDto;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.model.Address;
import com.ecommerce.user.model.UserProfile;
import com.ecommerce.user.repository.AddressRepository;
import com.ecommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private static final String CACHE_NAME = "user-profiles";

    private final UserProfileRepository userProfileRepository;
    private final AddressRepository addressRepository;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#userId")
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        log.info("Fetching profile from DB for userId: {}", userId);
        UserProfile profile = findProfileByUserId(userId);
        return toDto(profile);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#userId")
    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for userId: {}", userId);
        UserProfile profile = findProfileByUserId(userId);

        if (request.firstName() != null) {
            profile.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            profile.setLastName(request.lastName());
        }
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.phone() != null) {
            profile.setPhone(request.phone());
        }

        UserProfile saved = userProfileRepository.save(profile);
        return toDto(saved);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#userId")
    @Transactional
    public AddressDto addAddress(UUID userId, AddressDto addressDto) {
        log.info("Adding address for userId: {}", userId);
        UserProfile profile = findProfileByUserId(userId);

        Address address = Address.builder()
                .street(addressDto.street())
                .city(addressDto.city())
                .state(addressDto.state())
                .zipCode(addressDto.zipCode())
                .country(addressDto.country())
                .isDefault(addressDto.isDefault())
                .userProfile(profile)
                .build();

        if (address.isDefault()) {
            profile.getAddresses().forEach(a -> a.setDefault(false));
        }

        Address saved = addressRepository.save(address);
        return toAddressDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses(UUID userId) {
        UserProfile profile = findProfileByUserId(userId);
        return addressRepository.findByUserProfileId(profile.getId())
                .stream()
                .map(this::toAddressDto)
                .toList();
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#userId")
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        log.info("Deleting address {} for userId: {}", addressId, userId);
        UserProfile profile = findProfileByUserId(userId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (!address.getUserProfile().getId().equals(profile.getId())) {
            throw new IllegalArgumentException("Address does not belong to this user");
        }

        addressRepository.delete(address);
    }

    private UserProfile findProfileByUserId(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));
    }

    private UserProfileDto toDto(UserProfile profile) {
        List<AddressDto> addressDtos = profile.getAddresses()
                .stream()
                .map(this::toAddressDto)
                .toList();

        return new UserProfileDto(
                profile.getId(),
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone(),
                addressDtos,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private AddressDto toAddressDto(Address address) {
        return new AddressDto(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry(),
                address.isDefault()
        );
    }
}
