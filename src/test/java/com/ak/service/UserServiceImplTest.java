package com.ak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ak.dto.CreateUserDto;
import com.ak.dto.UpdateUserDto;
import com.ak.dto.UserDto;
import com.ak.entity.Role;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private ModelMapper modelMapper;

	@Mock
	private RedisService redisService;

	@Mock
	private BCryptPasswordEncoder encoder;

	@InjectMocks
	private UserServiceImpl userService;

	private Users user;
	private UserDto userDto;
	private CreateUserDto createUserDto;
	private UpdateUserDto updateUserDto;

	@BeforeEach
	void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		user = new Users();
		user.setId(1L);
		user.setName("testuser");
		user.setEmail("test@example.com");
		user.setPassword("encodedPassword");
		user.setRole(Role.SEEKER);

		userDto = new UserDto();
		userDto.setId(1L);
		userDto.setName("testuser");
		userDto.setEmail("test@example.com");
		userDto.setRole(Role.SEEKER);

		createUserDto = new CreateUserDto();
		createUserDto.setName("testuser");
		createUserDto.setEmail("test@example.com");
		createUserDto.setPassword("password");
		createUserDto.setRole(("SEEKER"));

		updateUserDto = new UpdateUserDto();
		updateUserDto.setName("updateduser");
		updateUserDto.setEmail("updated@example.com");
		updateUserDto.setPassword("newpassword");
		updateUserDto.setRole("ADMIN");

		Field encoderField = UserServiceImpl.class.getDeclaredField("encoder");
		encoderField.setAccessible(true);
		encoderField.set(userService, encoder);

		// Mock the encoder since it's initialized in the service
		lenient().when(encoder.encode(anyString())).thenReturn("encodedPassword");
	}

	@Test
	void insertUser_ValidInput_ReturnsUserDto() {
		// Arrange
		Users user1 = new Users();
		user1.setId(1L);
		user1.setName("testuser");
		user1.setEmail("test@example.com");
		user1.setPassword("password");
		user1.setRole(Role.SEEKER);
		when(modelMapper.map(createUserDto, Users.class)).thenReturn(user1);
		when(userRepository.save(user1)).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

		// Act
		UserDto result = userService.insertUser(createUserDto);

		// Assert
		assertNotNull(result);
		assertEquals("testuser", result.getName());
		verify(encoder).encode("password");
		verify(userRepository).save(user);
	}

	@Test
	void getUserById_WhenExists_ReturnsUserDto() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);
		doNothing().when(redisService).setValue(eq("user:1"), eq(user), anyLong());

		// Act
		UserDto result = userService.getUserById(1L);

		// Assert
		assertNotNull(result);
		assertEquals(1L, result.getId());
		verify(redisService).getValue("user:1", Users.class);
		verify(userRepository).findById(1L);
	}

	@Test
	void getUserById_WhenCached_ReturnsFromCache() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

		// Act
		UserDto result = userService.getUserById(1L);

		// Assert
		assertNotNull(result);
		verify(redisService).getValue("user:1", Users.class);
		verifyNoInteractions(userRepository);
	}

	@Test
	void deleteUserById_ValidId_DeletesUser() {
		// Arrange
		doNothing().when(redisService).deleteKey("user:1");
		doNothing().when(userRepository).deleteById(1L);

		// Act
		userService.deleteUserById(1L);

		// Assert
		verify(redisService).deleteKey("user:1");
		verify(userRepository).deleteById(1L);
	}

	@Test
	void updateUser_AuthorizedUser_UpdatesAndReturnsDto() {
		// Arrange
		setupSecurityContext("testuser");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);
		doNothing().when(redisService).deleteKey("user:1");
		doNothing().when(redisService).setValue(eq("user:1"), any(), anyLong());

		// Act
		UserDto result = userService.updateUser(1L, updateUserDto);

		// Assert
		assertNotNull(result);
		verify(userRepository).save(user);
		verify(redisService).deleteKey("user:1");
		// Verify setValue was called exactly twice
		verify(redisService, times(2)).setValue(eq("user:1"), any(), eq(3600L));

		// Verify the first call (from findUserById)
		verify(redisService).setValue(eq("user:1"), eq(user), eq(3600L));

		// Verify the second call (from updateUser)
		verify(redisService).setValue(eq("user:1"), eq(userDto), eq(3600L));

	}

	@Test
	void updateUser_UnauthorizedUser_ThrowsException() {
		// Arrange
		setupSecurityContext("otheruser");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		// Act & Assert
		assertThrows(UnauthorizedAccessException.class, () -> {
			userService.updateUser(1L, updateUserDto);
		});
	}

	@Test
	void updateUser_PartialUpdate_UpdatesOnlyProvidedFields() {
		// Arrange
		setupSecurityContext("testuser");
		UpdateUserDto partialUpdate = new UpdateUserDto();
		partialUpdate.setEmail("new@example.com");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);
		doNothing().when(redisService).deleteKey("user:1");
		doNothing().when(redisService).setValue(eq("user:1"), any(), anyLong());

		// Act
		UserDto result = userService.updateUser(1L, partialUpdate);

		// Assert
		assertNotNull(result);
		assertEquals("new@example.com", user.getEmail());
		assertEquals("testuser", user.getName()); // Name should remain unchanged
		verify(encoder, never()).encode(anyString()); // Password not updated
	}

	@Test
	void getUserById_WhenNotExists_ThrowsException() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(null);
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			userService.getUserById(1L);
		});
	}

	@Test
	void findUserById_WhenNotExists_ThrowsException() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(null);
		when(userRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(ResourceNotFoundException.class, () -> {
			userService.findUserById(1L);
		});
	}

	@Test
	void insertUser_EncodesPassword() {
		// Arrange
		Users user1 = new Users();
		user1.setId(1L);
		user1.setName("testuser");
		user1.setEmail("test@example.com");
		user1.setPassword("password");
		user1.setRole(Role.SEEKER);
		when(modelMapper.map(createUserDto, Users.class)).thenReturn(user1);
		when(userRepository.save(user1)).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);

		// Act
		userService.insertUser(createUserDto);

		// Assert
		verify(encoder).encode("password");
		assertEquals("encodedPassword", user.getPassword());
	}

	@Test
	void updateUser_WhenPasswordChanged_EncodesNewPassword() {
		// Arrange
		setupSecurityContext("testuser");
		UpdateUserDto passwordUpdate = new UpdateUserDto();
		passwordUpdate.setPassword("newpassword");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);
		doNothing().when(redisService).deleteKey("user:1");
		doNothing().when(redisService).setValue(eq("user:1"), any(), anyLong());

		// Act
		userService.updateUser(1L, passwordUpdate);

		// Assert
		verify(encoder).encode("newpassword");
		assertEquals("encodedPassword", user.getPassword());
	}

	private void setupSecurityContext(String username) {
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn(username);

		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);

		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void findUserById_ReturnsFromCacheWhenAvailable() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(user);

		// Act
		Users result = userService.findUserById(1L);

		// Assert
		assertEquals(user, result);
		verifyNoInteractions(userRepository);
	}

	@Test
	void findUserById_FetchesFromRepositoryWhenCacheEmpty() {
		// Arrange
		when(redisService.getValue("user:1", Users.class)).thenReturn(null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		doNothing().when(redisService).setValue(eq("user:1"), eq(user), eq(3600L));

		// Act
		Users result = userService.findUserById(1L);

		// Assert
		assertEquals(user, result);
		verify(userRepository).findById(1L);
		verify(redisService).setValue("user:1", user, 3600L);
	}

	@Test
	void updateUser_ClearsUserCache() {
		// Arrange
		setupSecurityContext("testuser");

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenReturn(user);
		when(modelMapper.map(user, UserDto.class)).thenReturn(userDto);
		doNothing().when(redisService).deleteKey("user:1");

		// Act
		userService.updateUser(1L, updateUserDto);

		// Assert
		verify(redisService).deleteKey("user:1");
	}

	@Test
	void deleteUserById_ClearsUserCache() {
		// Arrange
		doNothing().when(redisService).deleteKey("user:1");
		doNothing().when(userRepository).deleteById(1L);

		// Act
		userService.deleteUserById(1L);

		// Assert
		verify(redisService).deleteKey("user:1");
	}

}