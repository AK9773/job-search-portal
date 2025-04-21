package com.ak.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.ApiResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;
import com.ak.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private AuthService authService;

	public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<UserDto>> login(@RequestBody LoginRequest loginRequest,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(authService.login(loginRequest, request, response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(HttpServletRequest request,
			HttpServletResponse response) {
		return ResponseEntity.ok(authService.refreshToken(request, response));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
		return ResponseEntity.ok(authService.logout(request, response));
	}
}