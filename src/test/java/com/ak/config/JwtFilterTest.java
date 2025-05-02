package com.ak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.ak.service.MyUserDetailsService;
import com.ak.utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private ApplicationContext context;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@Mock
	private MyUserDetailsService userDetailsService;

	@InjectMocks
	private JwtFilter jwtFilter;

	private static final String TEST_USERNAME = "testuser";
	private static final String VALID_TOKEN = "valid.token.here";
	private static final String EXPIRED_TOKEN = "expired.token.here";
	private static final String INVALID_TOKEN = "invalid.token.here";

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testDoFilterInternal_WithValidAuthorizationHeader() throws Exception {
		// Arrange
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
		when(jwtUtils.extractUsername(VALID_TOKEN, "secret")).thenReturn(TEST_USERNAME);

		UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
		when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
		when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
		when(jwtUtils.validateToken(VALID_TOKEN, userDetails)).thenReturn(true);

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verify(jwtUtils).extractUsername(VALID_TOKEN, "secret");
		verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
		verify(jwtUtils).validateToken(VALID_TOKEN, userDetails);
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void testDoFilterInternal_WithValidCookie() throws Exception {
		// Arrange
		Cookie cookie = new Cookie("jwtToken", VALID_TOKEN);
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(request.getCookies()).thenReturn(new Cookie[] { cookie });
		when(jwtUtils.extractUsername(VALID_TOKEN, "secret")).thenReturn(TEST_USERNAME);

		UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
		when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
		when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(userDetails);
		when(jwtUtils.validateToken(VALID_TOKEN, userDetails)).thenReturn(true);

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verify(jwtUtils).extractUsername(VALID_TOKEN, "secret");
		verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
		verify(jwtUtils).validateToken(VALID_TOKEN, userDetails);
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void testDoFilterInternal_WithExpiredTokenInHeader() throws Exception {
		// Arrange
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(request.getHeader("Authorization")).thenReturn("Bearer " + EXPIRED_TOKEN);
		when(jwtUtils.extractUsername(EXPIRED_TOKEN, "secret"))
				.thenThrow(new ExpiredJwtException(null, null, "Token expired"));

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verify(jwtUtils).extractUsername(EXPIRED_TOKEN, "secret");
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void testDoFilterInternal_WithInvalidTokenInCookie() throws Exception {
		// Arrange
		Cookie cookie = new Cookie("jwtToken", INVALID_TOKEN);
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(request.getCookies()).thenReturn(new Cookie[] { cookie });
		when(jwtUtils.extractUsername(INVALID_TOKEN, "secret")).thenThrow(new RuntimeException("Invalid token"));

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verify(jwtUtils).extractUsername(INVALID_TOKEN, "secret");
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void testDoFilterInternal_NoTokenPresent() throws Exception {
		// Arrange
		when(request.getHeader("Authorization")).thenReturn(null);
		when(request.getCookies()).thenReturn(null);

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verifyNoInteractions(jwtUtils);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);

	}

	@Test
	void testDoFilterInternal_AuthenticationAlreadySet() throws Exception {
		// Arrange

		UserDetails userDetails = new User(TEST_USERNAME, "password", Collections.emptyList());
		UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(existingAuth);

		when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(jwtUtils.extractUsername(VALID_TOKEN, "secret")).thenReturn(TEST_USERNAME);

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert

		verify(jwtUtils).extractUsername(VALID_TOKEN, "secret");
		verify(userDetailsService, never()).loadUserByUsername(any());
		assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void testDoFilterInternal_InvalidUser() throws Exception {
		// Arrange
		when(jwtUtils.getJwtSecret()).thenReturn("secret");
		when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
		when(jwtUtils.extractUsername(VALID_TOKEN, "secret")).thenReturn(TEST_USERNAME);

		when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
		when(userDetailsService.loadUserByUsername(TEST_USERNAME)).thenReturn(null);

		// Act
		jwtFilter.doFilterInternal(request, response, filterChain);

		// Assert
		verify(jwtUtils).extractUsername(VALID_TOKEN, "secret");
		verify(userDetailsService).loadUserByUsername(TEST_USERNAME);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(request, response);
	}
}