package com.ak.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ak.dto.ApiResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;
import com.ak.entity.Role;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	@Mock
	private AuthService authService;

	@InjectMocks
	private AuthController authController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
		objectMapper = new ObjectMapper();
	}

	@Test
	void testLogin_Success() throws Exception {
		// Arrange
		LoginRequest loginRequest = new LoginRequest("username", "Password@123");
		UserDto userDto = new UserDto(1L, "username", "email@example.com", Role.SEEKER);
		ApiResponse<UserDto> response = new ApiResponse<>(200, "Login successful", LocalDateTime.now(), userDto);

		when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(response);

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest))).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andExpect(jsonPath("$.data.name").value("username"));

		verify(authService).login(any(LoginRequest.class), any(), any());
	}

	@Test
	void testLogin_InvalidRequest() throws Exception {

		LoginRequest invalidRequest = new LoginRequest("", "");

		// Act & Assert
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest))).andExpect(status().isBadRequest());
	}

	@Test
	void testRefreshToken_Success() throws Exception {
		// Arrange
		RefreshTokenResponse refreshResponse = new RefreshTokenResponse(1L, "username", "email@example.com",
				Role.SEEKER, "newRefreshToken");
		ApiResponse<RefreshTokenResponse> response = new ApiResponse<>(200, "Token refreshed", LocalDateTime.now(),
				refreshResponse);

		when(authService.refreshToken(any(), any(), any())).thenReturn(response);

		// Add refresh token cookie
		Cookie refreshCookie = new Cookie("refreshToken", "newRefreshToken");

		// Act & Assert
		mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie)).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Token refreshed"))
				.andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));

		verify(authService).refreshToken(any(), any(), any());
	}

	@Test
	void testRefreshToken_MissingCookie() throws Exception {
		// Act & Assert
		mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Cookies does not have Refresh Token"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void testLogout_Success() throws Exception {
		// Arrange
		ApiResponse<String> response = new ApiResponse<>(200, "Logged out successfully", LocalDateTime.now(), null);

		when(authService.logout(any(), any())).thenReturn(response);

		// Add refresh token cookie
		Cookie refreshCookie = new Cookie("refreshToken", "validRefreshToken");

		// Act & Assert
		mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie)).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logged out successfully"));

		verify(authService).logout(any(), any());
	}

	@Test
	void testLogout_NoCookies() throws Exception {
		// Act & Assert
		mockMvc.perform(post("/api/auth/logout")).andExpect(status().isOk());
	}
}