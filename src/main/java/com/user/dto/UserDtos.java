package com.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;

        @Size(max = 20)
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressRequest {
        @Size(max = 50)
        private String title;

        @Size(max = 50)
        private String province;

        @NotBlank
        @Size(max = 50)
        private String city;

        @NotBlank
        @Size(max = 255)
        private String street;

        @Size(max = 20)
        private String postalCode;
    }
}