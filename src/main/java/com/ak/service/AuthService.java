package com.ak.service;

import com.ak.dto.ApiResponse;
import com.ak.dto.LoginRequest;
import com.ak.dto.RefreshTokenResponse;
import com.ak.dto.UserDto;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
	ApiResponse<UserDto> login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response);

	ApiResponse<RefreshTokenResponse> refreshToken(HttpServletRequest request, HttpServletResponse response,
			Cookie[] cookies);

	ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response);
}