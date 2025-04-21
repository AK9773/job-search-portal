package com.ak.dto;

import com.ak.entity.Role;

import lombok.Data;
@Data
public class RefreshTokenResponse {
	
	private Long id;
	private String name;
	private String email;
	private Role role;
	private String refreshToken;
	
}
