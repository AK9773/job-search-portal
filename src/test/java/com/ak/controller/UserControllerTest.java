package com.ak.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ak.dto.CreateUserDto;
import com.ak.dto.UpdateUserDto;
import com.ak.dto.UserDto;
import com.ak.entity.Role;
import com.ak.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController userController;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		mockMvc = MockMvcBuilders.standaloneSetup(userController)
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
	}

	@Test
	void insertUser_ShouldReturnCreatedResponse() throws Exception {
		// Arrange
		CreateUserDto createUserDto = new CreateUserDto("testuser", "test@example.com", "Password@123", "SEEKER");
		UserDto userDto = new UserDto(1L, "testuser", "test@example.com", Role.SEEKER);

		when(userService.insertUser(any(CreateUserDto.class))).thenReturn(userDto);

		// Act & Assert
		mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createUserDto))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.message").value("User record Created successfully"))
				.andExpect(jsonPath("$.data.name").value("testuser"))
				.andExpect(jsonPath("$.data.email").value("test@example.com"));
	}

	@Test
	void getUserById_ShouldReturnUser() throws Exception {
		// Arrange
		UserDto userDto = new UserDto(1L, "testuser", "test@example.com", Role.SEEKER);

		when(userService.getUserById(1L)).thenReturn(userDto);

		// Act & Assert
		mockMvc.perform(get("/api/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("User record fetched successfully"))
				.andExpect(jsonPath("$.data.id").value(1L)).andExpect(jsonPath("$.data.name").value("testuser"));
	}

	@Test
	void updateUser_ShouldReturnUpdatedUser() throws Exception {
		// Arrange
		UpdateUserDto updateUserDto = new UpdateUserDto(null, "updated@example.com", null, null);
		UserDto userDto = new UserDto(1L, "testuser", "updated@example.com", Role.SEEKER);

		when(userService.updateUser(anyLong(), any(UpdateUserDto.class))).thenReturn(userDto);

		// Act & Assert
		mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateUserDto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("User record updated successfully"))
				.andExpect(jsonPath("$.data.email").value("updated@example.com"))
				.andExpect(jsonPath("$.data.name").value("testuser"));
	}

	@Test
	void deleteUserById_ShouldReturnSuccessMessage() throws Exception {
		// Arrange - No need to mock anything for void method unless it throws exception

		// Act & Assert
		mockMvc.perform(delete("/api/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("User record deleted successfully"))
				.andExpect(jsonPath("$.data").value("")).andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void insertUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
		// Arrange
		CreateUserDto invalidUser = new CreateUserDto("", "", "invalid-email", "");

		// Act & Assert
		mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidUser))).andExpect(status().isBadRequest());
	}
}