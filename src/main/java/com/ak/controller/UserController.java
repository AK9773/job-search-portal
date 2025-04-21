package com.ak.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ak.dto.ApiResponse;
import com.ak.dto.CreateUserDto;
import com.ak.dto.UpdateUserDto;
import com.ak.dto.UserDto;
import com.ak.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/api/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserDto>> insertUser(@RequestBody CreateUserDto user) {
		return new ResponseEntity<ApiResponse<UserDto>>(new ApiResponse<UserDto>(201,
				"User record Created successfully", LocalDateTime.now(), userService.insertUser(user)),
				HttpStatus.CREATED);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
		return new ResponseEntity<ApiResponse<UserDto>>(new ApiResponse<UserDto>(200,
				"User record fetched successfully", LocalDateTime.now(), userService.getUserById(id)), HttpStatus.OK);
	}

	@PutMapping("/{userId}")
	public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable long userId,
			@Valid @RequestBody UpdateUserDto user) {
		userService.updateUser(userId, user);
		return new ResponseEntity<ApiResponse<UserDto>>(new ApiResponse<UserDto>(200,
				"User record updated successfully", LocalDateTime.now(), userService.updateUser(userId, user)),
				HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<String>> deleteUserById(@PathVariable Long id) {
		userService.deleteUserById(id);
		return new ResponseEntity<ApiResponse<String>>(
				new ApiResponse<String>(200, "User record deleted successfully", LocalDateTime.now(), ""),
				HttpStatus.OK);
	}

}
