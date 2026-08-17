package com.user.controller;

import com.user.dto.AddressDtos.AddressResponse;
import com.user.dto.AuthDtos.UserDto;
import com.user.dto.UserDtos.AddressRequest;
import com.user.dto.UserDtos.UpdateProfileRequest;
import com.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserDto getProfile(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserDto updateProfile(Authentication authentication,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addAddress(Authentication authentication,
                                                      @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.addAddress(authentication.getName(), request));
    }

    @GetMapping("/me/addresses")
    public List<AddressResponse> listAddresses(Authentication authentication) {
        return userService.listAddresses(authentication.getName());
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(Authentication authentication,
                                              @PathVariable Long addressId) {
        userService.deleteAddress(authentication.getName(), addressId);
        return ResponseEntity.noContent().build();
    }
}