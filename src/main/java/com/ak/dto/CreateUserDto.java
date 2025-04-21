package com.ak.dto;

import com.ak.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserDto {
	@Size(min = 2, max = 50, message = "Name must be between 2-50 characters")
	@NotNull(message = "Name cannot be null")
	private String name;

	@Email(message = "Email should be valid")
	@NotNull(message = "Email cannot be null")
	private String email;

	@Size(min = 8, message = "Password must be at least 8 characters")
	@NotNull(message = "Password cannot be null")
	private String password;

	@NotNull(message = "Role cannot be null")
	private Role role;
}
