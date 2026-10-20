package com.user.service;

import com.user.dto.AddressDtos.AddressResponse;
import com.user.dto.AuthDtos.UserDto;
import com.user.dto.UserDtos.AddressRequest;
import com.user.dto.UserDtos.UpdateProfileRequest;
import com.user.model.Address;
import com.user.model.User;
import com.user.model.enums.Role;
import com.user.repository.AddressRepository;
import com.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("12345")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void getProfile_returnsDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto dto = userService.getProfile("1");

        assertThat(dto.getUsername()).isEqualTo("johndoe");
        assertThat(dto.getRole()).isEqualTo("USER");
    }

    @Test
    void getProfile_throwsWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("99"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateProfile_updatesOnlyProvidedFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder().firstName("Jane").build();
        UserDto dto = userService.updateProfile("1", request);

        assertThat(dto.getFirstName()).isEqualTo("Jane");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getPhone()).isEqualTo("12345");
    }

    @Test
    void addAddress_savesAddressForUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        AddressRequest request = AddressRequest.builder()
                .city("Tehran").street("Main St").postalCode("12345").build();

        AddressResponse response = userService.addAddress("1", request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCity()).isEqualTo("Tehran");
    }

    @Test
    void listAddresses_returnsAllForUser() {
        Address address = Address.builder().id(5L).user(user).city("Tehran").street("Main St").build();
        when(addressRepository.findByUserId(1L)).thenReturn(List.of(address));

        List<AddressResponse> responses = userService.listAddresses("1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(5L);
    }

    @Test
    void deleteAddress_delegatesToRepository() {
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> addressIdCaptor = ArgumentCaptor.forClass(Long.class);

        userService.deleteAddress("1", 5L);

        verify(addressRepository).deleteByUserIdAndId(userIdCaptor.capture(), addressIdCaptor.capture());
        assertThat(userIdCaptor.getValue()).isEqualTo(1L);
        assertThat(addressIdCaptor.getValue()).isEqualTo(5L);
    }
}
