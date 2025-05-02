package com.ak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.ak.dto.ApiResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.repository.UserRepository;
import com.ak.utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuthenticationManager authManager;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private ApplicationContext context;

	@Mock
	private ModelMapper modelMapper;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private MyUserDetailsService userDetailsService;

	@InjectMocks
	private AuthServiceImpl authService;

	private Users testUser;
	private UserDto userDto;
	private LoginRequest loginRequest;

	@BeforeEach
	void setUp() {
		testUser = new Users();
		testUser.setName("testuser");
		testUser.setPassword("password");

		userDto = new UserDto();
		userDto.setName("testuser");

		loginRequest = new LoginRequest("testuser", "password");

		lenient().when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
	}

	@Test
	void login_SuccessfulAuthentication_ReturnsSuccessResponse() {
		// Arrange
		Authentication auth = mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(authManager.authenticate(any())).thenReturn(auth);
		when(userRepository.findByName(anyString())).thenReturn(Collections.singletonList(testUser));
		when(modelMapper.map(any(), eq(UserDto.class))).thenReturn(userDto);

		// Act
		ApiResponse<UserDto> apiResponse = authService.login(loginRequest, request, response);

		// Assert
		assertEquals(200, apiResponse.getStatus());
		assertEquals("Logged in Successfull", apiResponse.getMessage());
		assertNotNull(apiResponse.getData());
		verify(response, atLeast(2)).addCookie(any());
		verify(userRepository).save(any());
	}

	@Test
	void login_FailedAuthentication_ThrowsUnauthorizedAccessException() {
		// Arrange
		when(authManager.authenticate(any())).thenThrow(new UnauthorizedAccessException("Invalid Credential"));

		// Act & Assert
		assertThrows(UnauthorizedAccessException.class, () -> {
			authService.login(loginRequest, request, response);
		});
	}

	@Test
	void login_UserNotFound_ThrowsUnauthorizedAccessException() {
		// Arrange
		Authentication auth = mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(authManager.authenticate(any())).thenReturn(auth);
		when(userRepository.findByName(loginRequest.getName())).thenReturn(Collections.emptyList());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			authService.login(loginRequest, request, response);
		});
	}

	@Test
	void refreshToken_ValidRefreshToken_ReturnsNewTokens() {
		// Arrange
		String refreshToken = "valid.refresh.token";
		String username = "testuser";

		when(jwtUtils.getRefreshSecret()).thenReturn("valid refresh secret");
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("refreshToken", refreshToken) });
		when(jwtUtils.extractUsername(refreshToken, "valid refresh secret")).thenReturn(username);
		when(userRepository.findByName(username)).thenReturn(Collections.singletonList(testUser));
		when(userDetailsService.loadUserByUsername(username)).thenReturn(mock(UserDetails.class));
		when(jwtUtils.validateRefreshToken(eq(refreshToken), any())).thenReturn(true);
		when(modelMapper.map(any(), eq(RefreshTokenResponse.class))).thenReturn(new RefreshTokenResponse());

		// Act
		Cookie[] cookies = request.getCookies();
		ApiResponse<RefreshTokenResponse> apiResponse = authService.refreshToken(request, response, cookies);

		// Assert
		assertEquals(200, apiResponse.getStatus());
		assertEquals("Access Tokens refreshed", apiResponse.getMessage());
		verify(response, atLeast(2)).addCookie(any());
		verify(userRepository).save(any());
	}

	@Test
	void refreshToken_ExpiredRefreshToken_ReturnsExpiredResponse() {
		// Arrange
		String refreshToken = "expired.refresh.token";

		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("refreshToken", refreshToken) });
		when(jwtUtils.extractUsername(refreshToken, jwtUtils.getRefreshSecret()))
				.thenThrow(new ExpiredJwtException(null, null, "Token expired"));

		// Act
		Cookie[] cookies = request.getCookies();
		ApiResponse<RefreshTokenResponse> apiResponse = authService.refreshToken(request, response, cookies);

		// Assert
		assertEquals(401, apiResponse.getStatus());
		assertEquals("Refresh token expired", apiResponse.getMessage());
	}

	@Test
	void refreshToken_NoRefreshToken_ReturnsNotFoundResponse() {
		// Arrange
		when(request.getCookies()).thenReturn(new Cookie[0]);

		// Act
		Cookie[] cookies = request.getCookies();

		// Assert
		assertThrows(UnauthorizedAccessException.class, () -> {
			authService.refreshToken(request, response, cookies);
		});

	}

	@Test
	void refreshToken_InvalidRefreshToken_ReturnsInvalidResponse() {
		// Arrange
		String refreshToken = "invalid.refresh.token";

		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("refreshToken", refreshToken) });
		when(jwtUtils.extractUsername(refreshToken, jwtUtils.getRefreshSecret())).thenReturn("testuser");
		when(userRepository.findByName("testuser")).thenReturn(Collections.singletonList(testUser));
		when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mock(UserDetails.class));
		when(jwtUtils.validateRefreshToken(eq(refreshToken), any())).thenReturn(false);

		// Act
		Cookie[] cookies = request.getCookies();
		ApiResponse<RefreshTokenResponse> apiResponse = authService.refreshToken(request, response, cookies);

		// Assert
		assertEquals(401, apiResponse.getStatus());
		assertEquals("Invalid refresh token", apiResponse.getMessage());
	}

	@Test
	void logout_ValidUser_ClearsTokensAndContext() {
		// Arrange
		Authentication auth = mock(Authentication.class);
		when(auth.getName()).thenReturn("testuser");
		SecurityContextHolder.getContext().setAuthentication(auth);

		when(userRepository.findByName("testuser")).thenReturn(Collections.singletonList(testUser));

		// Act
		ApiResponse<String> apiResponse = authService.logout(request, response);

		// Assert
		assertEquals(200, apiResponse.getStatus());
		assertEquals("Logout successful", apiResponse.getMessage());
		verify(response, times(2)).addCookie(any());
		verify(userRepository).save(any());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void logout_NoAuthenticatedUser_ThrowsUnauthorizedAccessException() {
		// Arrange
		SecurityContextHolder.clearContext();

		// Assert
		assertThrows(UnauthorizedAccessException.class, () -> {
			authService.logout(request, response);
		});
	}

}
