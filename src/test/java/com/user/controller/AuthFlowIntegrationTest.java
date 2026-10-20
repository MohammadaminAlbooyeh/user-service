package com.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.dto.AuthDtos.LoginRequest;
import com.user.dto.AuthDtos.RefreshRequest;
import com.user.dto.AuthDtos.RegisterRequest;
import com.user.dto.UserDtos.AddressRequest;
import com.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Test
    void registerThenLoginThenAccessProfile() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .firstName("Alice")
                .build();

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerBody).get("token").asText();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        LoginRequest loginRequest = LoginRequest.builder().username("alice").password("password123").build();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void refreshThenLogout_rotatesAndRevokesToken() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("dave")
                .email("dave@example.com")
                .password("password123")
                .build();

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(registerBody).get("refreshToken").asText();

        String refreshedBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                RefreshRequest.builder().refreshToken(refreshToken).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();
        String newRefreshToken = objectMapper.readTree(refreshedBody).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                RefreshRequest.builder().refreshToken(refreshToken).build())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                RefreshRequest.builder().refreshToken(newRefreshToken).build())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                RefreshRequest.builder().refreshToken(newRefreshToken).build())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withDuplicateUsername_returnsBadRequest() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("bob")
                .email("bob@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void accessingProtectedEndpoint_withoutToken_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void addressLifecycle_createListAndDelete() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("carol")
                .email("carol@example.com")
                .password("password123")
                .build();

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerBody).get("token").asText();

        AddressRequest addressRequest = AddressRequest.builder()
                .city("Tehran").street("Main St").postalCode("12345").build();

        String addressBody = mockMvc.perform(post("/api/users/me/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long addressId = objectMapper.readTree(addressBody).get("id").asLong();

        mockMvc.perform(get("/api/users/me/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/users/me/addresses/" + addressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
