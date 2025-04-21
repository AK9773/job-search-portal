package com.ak.service;

import java.time.LocalDateTime;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ak.dto.ApiResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;
import com.ak.entity.Users;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.repository.UserRepository;
import com.ak.utils.JwtUtils;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthServiceImpl implements AuthService {

	private UserRepository userRepository;
	private AuthenticationManager authManager;
	private JwtUtils jwtUtils;
	ApplicationContext context;
	private ModelMapper modelMapper;

	public AuthServiceImpl(UserRepository userRepository, AuthenticationManager authManager, JwtUtils jwtUtils,
			ApplicationContext context, ModelMapper modelMapper) {
		this.userRepository = userRepository;
		this.authManager = authManager;
		this.jwtUtils = jwtUtils;
		this.context = context;
		this.modelMapper = modelMapper;
	}

	@Override
	public ApiResponse<UserDto> login(LoginRequest loginRequest, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication = authManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getName(), loginRequest.getPassword()));
		if (authentication.isAuthenticated()) {
			Users byName = userRepository.findByName(loginRequest.getName()).get(0);
			addCookie(byName.getName(), request, response, "jwtToken");
			String refreshToken = addCookie(byName.getName(), request, response, "refreshToken");
			byName.setRefreshToken(refreshToken);
			Users savedUser = userRepository.save(byName);
			UserDto userDto = modelMapper.map(savedUser, UserDto.class);
			return new ApiResponse<UserDto>(200, "Logged in Successfull", LocalDateTime.now(), userDto);
		} else {
			throw new UnauthorizedAccessException("Invalid Credential");
		}
	}

	@Override
	public ApiResponse<RefreshTokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {

		String refreshToken = null;
		String username = null;
		Cookie[] cookies = request.getCookies();

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refreshToken".equals(cookie.getName()) && cookie.getValue() != null) {
					refreshToken = cookie.getValue().trim();
					if (!refreshToken.isEmpty()) {
						try {
							username = jwtUtils.extractUsername(refreshToken, jwtUtils.getRefreshSecret());
						} catch (ExpiredJwtException e) {
							return new ApiResponse<>(401, "Refresh token expired", LocalDateTime.now(), null);
						} catch (Exception e) {
							return new ApiResponse<>(401, "Invalid refresh token", LocalDateTime.now(), null);
						}
					}
					break;
				}
			}
		}

		if (username == null || username.isEmpty()) {
			return new ApiResponse<>(401, "Refresh token not found or invalid", LocalDateTime.now(), null);
		}

		List<Users> users = userRepository.findByName(username);
		if (users.isEmpty()) {
			return new ApiResponse<>(404, "User not found", LocalDateTime.now(), null);
		}

		Users user = users.get(0);
		UserDetails userDetails = context.getBean(MyUserDetailsService.class).loadUserByUsername(username);

		if (!jwtUtils.validateRefreshToken(refreshToken, userDetails)) {
			return new ApiResponse<>(401, "Invalid refresh token", LocalDateTime.now(), null);
		}

		try {
			addCookie(user.getName(), request, response, "jwtToken");
			String newRefreshToken = addCookie(user.getName(), request, response, "refreshToken");
			user.setRefreshToken(newRefreshToken);
			Users savedUser = userRepository.save(user);
			RefreshTokenResponse refreshTokenResponse = modelMapper.map(savedUser, RefreshTokenResponse.class);
			return new ApiResponse<>(200, "Access Tokens refreshed", LocalDateTime.now(), refreshTokenResponse);
		} catch (Exception e) {
			return new ApiResponse<>(500, "Failed to refresh tokens", LocalDateTime.now(), null);
		}
	}

	@Override
	public ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
		String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
		Users loggedInUser = userRepository.findByName(loggedInUsername).get(0);
		loggedInUser.setRefreshToken(null);
		userRepository.save(loggedInUser);
		// Remove JWT token cookie
		removeCookie(request, response, "jwtToken");
		// Remove refresh token cookie
		removeCookie(request, response, "refreshToken");

		// Optionally clear the security context
		SecurityContextHolder.clearContext();

		return new ApiResponse<>(200, "Logout successful", LocalDateTime.now(), "");
	}

	private void removeCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(0); // Immediately expire the cookie
		cookie.setSecure(request.isSecure()); // Match the secure flag with current request
		response.addCookie(cookie);
	}

	private String addCookie(String username, HttpServletRequest request, HttpServletResponse response,
			String tokenName) {
		String token = tokenName.equals("jwtToken") ? jwtUtils.generateJwtToken(username)
				: jwtUtils.generateRefreshToken(username);

		Cookie cookie = new Cookie(tokenName, token);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(24 * 60 * 60);
		cookie.setSecure(request.isSecure()); // Set secure based on current request
		response.addCookie(cookie);

		return token;
	}

}