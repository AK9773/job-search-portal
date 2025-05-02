package com.ak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

	@Mock
	private JwtFilter jwtFilter;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private AuthenticationConfiguration authenticationConfiguration;

	@Mock
	private HttpSecurity httpSecurity;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private SecurityConfig securityConfig;

	@BeforeEach
	void setUp() throws Exception {
		// Mock HttpSecurity behavior
		lenient().when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
		lenient().when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
		lenient().when(httpSecurity.httpBasic(any())).thenReturn(httpSecurity);
		lenient().when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
		lenient().when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
		DefaultSecurityFilterChain mockFilterChain = mock(DefaultSecurityFilterChain.class);
		lenient().when(httpSecurity.build()).thenReturn(mockFilterChain);

	}

	@Test
	void testSecurityFilterChain() throws Exception {
		// Act
		SecurityFilterChain filterChain = securityConfig.securityFilterChain(httpSecurity);

		// Assert
		assertNotNull(filterChain);

		// Verify exact method calls
		verify(httpSecurity).csrf(argThat(customizer -> true)); // Simplified verification
		verify(httpSecurity).authorizeHttpRequests(argThat(requests -> true));
		verify(httpSecurity).httpBasic(argThat(customizer -> true));
		verify(httpSecurity).sessionManagement(argThat(session -> true));
		verify(httpSecurity).addFilterBefore(any(JwtFilter.class), eq(UsernamePasswordAuthenticationFilter.class));
	}

	@Test
	void testAuthenticationProvider() {
		// 1. Get the provider and verify its type
		AuthenticationProvider provider = securityConfig.authenticationProvider();
		assertTrue(provider instanceof DaoAuthenticationProvider);

		// 2. Configure the provider with our mocks
		// This is CRUCIAL - the provider needs our mocked instances
		((DaoAuthenticationProvider) provider).setUserDetailsService(userDetailsService);
		((DaoAuthenticationProvider) provider).setPasswordEncoder(passwordEncoder);

		// 3. Setup test data
		String username = "test";
		String rawPassword = "test";
		String encodedPassword = "encodedTestPassword";

		UserDetails user = User.withUsername(username).password(encodedPassword).authorities(Collections.emptyList())
				.build();

		// 4. Configure mock behaviors
		when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
		when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

		// 5. Perform authentication
		Authentication authentication = new UsernamePasswordAuthenticationToken(username, rawPassword);
		Authentication result = provider.authenticate(authentication);

		// 6. Verify results
		assertNotNull(result);
		assertTrue(result.isAuthenticated());
		assertEquals(username, result.getName());

		// 7. Verify mock interactions
		verify(userDetailsService).loadUserByUsername(username);
		verify(passwordEncoder).matches(rawPassword, encodedPassword);
	}

	@Test
	void testAuthenticationManager() throws Exception {
		// Arrange
		AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
		when(authenticationConfiguration.getAuthenticationManager()).thenReturn(mockAuthManager);

		// Act
		AuthenticationManager authManager = securityConfig.authenticationManager(authenticationConfiguration);

		// Assert
		assertNotNull(authManager);
		assertEquals(mockAuthManager, authManager);
	}

	@Test
	void testPasswordEncoderStrength() throws NoSuchFieldException, IllegalAccessException {
		// Act: Get the authentication provider
		AuthenticationProvider provider = securityConfig.authenticationProvider();
		assertTrue(provider instanceof DaoAuthenticationProvider);
		DaoAuthenticationProvider daoProvider = (DaoAuthenticationProvider) provider;

		// Use reflection to access the private passwordEncoder field
		Field passwordEncoderField = DaoAuthenticationProvider.class.getDeclaredField("passwordEncoder");
		passwordEncoderField.setAccessible(true);
		PasswordEncoder encoder = (PasswordEncoder) passwordEncoderField.get(daoProvider);
		assertTrue(encoder instanceof BCryptPasswordEncoder);

		BCryptPasswordEncoder bCryptEncoder = (BCryptPasswordEncoder) encoder;

		// Extract the strength (log rounds) using reflection
		Field strengthField = BCryptPasswordEncoder.class.getDeclaredField("strength");
		strengthField.setAccessible(true);
		int strength = (int) strengthField.get(bCryptEncoder);

		// Assert: Verify strength is 12
		assertEquals(12, strength);

		// Optional: Verify password encoding works
		String rawPassword = "testPassword";
		String encodedPassword = bCryptEncoder.encode(rawPassword);
		assertTrue(bCryptEncoder.matches(rawPassword, encodedPassword));
	}
}