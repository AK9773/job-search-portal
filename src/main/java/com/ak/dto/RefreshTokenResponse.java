package com.ak.dto;

import com.ak.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponse {
	
	private Long id;
	private String name;
	private String email;
	private Role role;
	private String refreshToken;
	
}
