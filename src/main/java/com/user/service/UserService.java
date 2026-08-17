package com.user.service;

import com.user.dto.AddressDtos.AddressResponse;
import com.user.dto.AuthDtos.UserDto;
import com.user.dto.UserDtos.AddressRequest;
import com.user.dto.UserDtos.UpdateProfileRequest;
import com.user.model.Address;
import com.user.model.User;
import com.user.repository.AddressRepository;
import com.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public UserDto getProfile(String userId) {
        return toUserDto(findUser(Long.parseLong(userId)));
    }

    @Transactional
    public UserDto updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(Long.parseLong(userId));
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        return toUserDto(userRepository.save(user));
    }

    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {
        User user = findUser(Long.parseLong(userId));
        Address address = addressRepository.save(Address.builder()
                .user(user)
                .title(request.getTitle())
                .province(request.getProvince())
                .city(request.getCity())
                .street(request.getStreet())
                .postalCode(request.getPostalCode())
                .build());
        return toAddressResponse(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(String userId) {
        return addressRepository.findByUserId(Long.parseLong(userId)).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public void deleteAddress(String userId, Long addressId) {
        addressRepository.deleteByUserIdAndId(Long.parseLong(userId), addressId);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .title(address.getTitle())
                .province(address.getProvince())
                .city(address.getCity())
                .street(address.getStreet())
                .postalCode(address.getPostalCode())
                .build();
    }
}