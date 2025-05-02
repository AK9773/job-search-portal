package com.ak.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserDto {
	@Size(min = 2, max = 50, message = "Name must be between 2-50 characters")
	@NotNull(message = "Name cannot be null")
	private String name;

	@Email(message = "Email should be valid")
	@NotNull(message = "Email cannot be null")
	private String email;

	@Size(min = 8, message = "Password must be at least 8 characters")
	@NotNull(message = "Password cannot be null")
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character")
	private String password;

	@NotNull(message = "Role cannot be null")
	@Pattern(regexp = "SEEKER|EMPLOYER|ADMIN", message = "Invalid role. Allowed values: ADMIN, EMPLOYER, SEEKER")
	private String role;
}
