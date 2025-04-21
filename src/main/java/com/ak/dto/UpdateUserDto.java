package com.ak.dto;

import com.ak.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDto {

	@Size(min = 2, max = 50, message = "Name must be between 2-50 characters")
	private String name;

	@Email(message = "Email should be valid")
	private String email;

	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;

	private Role role;
}
