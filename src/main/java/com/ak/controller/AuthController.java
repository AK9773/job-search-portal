package com.ak.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.ApiResponse;
import com.ak.dto.ErrorResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private AuthService authService;

	public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<UserDto>> login(@Valid @RequestBody LoginRequest loginRequest,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(authService.login(loginRequest, request, response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
		try {
			Cookie[] cookies = request.getCookies();
			if (cookies == null) {
				throw new UnauthorizedAccessException("Cookies does not have Refresh Token");
			}

			ApiResponse<RefreshTokenResponse> apiResponse = authService.refreshToken(request, response, cookies);
			return ResponseEntity.ok(apiResponse);
		} catch (UnauthorizedAccessException ex) {
			ErrorResponse error = new ErrorResponse(401, ex.getMessage(), LocalDateTime.now());
			return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(authService.logout(request, response));
	}
}