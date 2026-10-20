package com.user.service;

import com.user.dto.AuthDtos.AuthResponse;
import com.user.dto.AuthDtos.LoginRequest;
import com.user.dto.AuthDtos.RegisterRequest;
import com.user.event.UserEventPublisher;
import com.user.model.RefreshToken;
import com.user.model.User;
import com.user.model.enums.Role;
import com.user.repository.RefreshTokenRepository;
import com.user.repository.UserRepository;
import com.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604_800_000L);
        registerRequest = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken rt = invocation.getArgument(0);
            rt.setId(1L);
            return rt;
        });
    }

    @Test
    void register_savesUserAndPublishesEvent() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(tokenProvider.generateToken("1", Role.USER)).thenReturn("token-abc");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getUser().getUsername()).isEqualTo("johndoe");
        verify(userEventPublisher).publishUserRegistered(any(User.class));
    }

    @Test
    void register_throwsWhenUsernameTaken() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verifyNoInteractions(userEventPublisher);
    }

    @Test
    void register_throwsWhenEmailTaken() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void login_succeedsWithValidCredentials() {
        User user = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .passwordHash("hashed")
                .role(Role.USER)
                .enabled(true)
                .build();
        LoginRequest loginRequest = LoginRequest.builder().username("johndoe").password("password123").build();

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(tokenProvider.generateToken("1", Role.USER)).thenReturn("token-abc");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("token-abc");
    }

    @Test
    void login_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        LoginRequest loginRequest = LoginRequest.builder().username("nobody").password("whatever").build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_throwsWhenPasswordWrong() {
        User user = User.builder()
                .id(1L).username("johndoe").passwordHash("hashed").role(Role.USER).enabled(true).build();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        LoginRequest loginRequest = LoginRequest.builder().username("johndoe").password("wrong").build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_throwsWhenUserDisabled() {
        User user = User.builder()
                .id(1L).username("johndoe").passwordHash("hashed").role(Role.USER).enabled(false).build();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        LoginRequest loginRequest = LoginRequest.builder().username("johndoe").password("password123").build();

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void refresh_rotatesValidToken() {
        User user = User.builder().id(1L).username("johndoe").role(Role.USER).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L).token("old-token").user(user).revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(refreshToken));
        when(tokenProvider.generateToken("1", Role.USER)).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("old-token");

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(refreshToken.getRevoked()).isTrue();
    }

    @Test
    void refresh_throwsWhenTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_throwsWhenTokenExpired() {
        User user = User.builder().id(1L).username("johndoe").role(Role.USER).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L).token("old-token").user(user).revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("old-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void logout_revokesToken() {
        User user = User.builder().id(1L).username("johndoe").role(Role.USER).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L).token("token").user(user).revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(refreshToken));

        authService.logout("token");

        assertThat(refreshToken.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
    }
}
