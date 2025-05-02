package com.ak.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDto {

	@Size(min = 2, max = 50, message = "Name must be between 2-50 characters")
	private String name;

	@Email(message = "Email should be valid")
	private String email;

	@Size(min = 8, message = "Password must be at least 8 characters")
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", message = "Password must contain at least one digit, one lowercase, one uppercase letter, and one special character")
	private String password;

	@Pattern(regexp = "SEEKER|EMPLOYER|ADMIN", message = "Invalid role. Allowed values: ADMIN, EMPLOYER, SEEKER")
	private String role;
}
