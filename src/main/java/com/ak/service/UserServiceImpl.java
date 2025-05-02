package com.ak.service;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ak.dto.CreateUserDto;
import com.ak.dto.UpdateUserDto;
import com.ak.dto.UserDto;
import com.ak.entity.Role;
import com.ak.entity.Users;
import com.ak.exception.ResourceNotFoundException;
import com.ak.exception.UnauthorizedAccessException;
import com.ak.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	private UserRepository userRepository;
	private ModelMapper modelMapper;
	private RedisService redisService;
	private BCryptPasswordEncoder encoder;

	public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, RedisService redisService,
			BCryptPasswordEncoder encoder) {
		this.userRepository = userRepository;
		this.modelMapper = modelMapper;
		this.redisService = redisService;
		this.encoder = encoder;
	}

	@Override
	public UserDto insertUser(CreateUserDto userDto) {
		Users user = modelMapper.map(userDto, Users.class);
		user.setRole(Role.valueOf(userDto.getRole().toUpperCase()));
		user.setPassword(encoder.encode(user.getPassword()));
		Users savedUser = userRepository.save(user);
		UserDto dto = modelMapper.map(savedUser, UserDto.class);
		return dto;
	}

	@Override
	public UserDto updateUser(long userId, UpdateUserDto userDto) {
		Users existingUser = this.findUserById(userId);
		String loggedInUsername = SecurityContextHolder.getContext().getAuthentication().getName();
		if (!loggedInUsername.equals(existingUser.getName())) {
			System.out.println(existingUser.getName());
			throw new UnauthorizedAccessException("Unauthorized to update this user");
		}
		if (userDto.getName() != null) {
			existingUser.setName(userDto.getName());
		}
		if (userDto.getEmail() != null) {
			existingUser.setEmail(userDto.getEmail());
		}
		if (userDto.getPassword() != null) {
			existingUser.setPassword(encoder.encode(userDto.getPassword()));
		}
		if (userDto.getRole() != null) {
			existingUser.setRole(Role.valueOf(userDto.getRole().toUpperCase()));
		}
		Users updatedUser = userRepository.save(existingUser);
		
		UserDto updatedUserDto = modelMapper.map(updatedUser, UserDto.class);
		
		redisService.deleteKey("user:" + updatedUser.getId());
		redisService.setValue("user:" + updatedUser.getId(), updatedUserDto, 3600L);
		return updatedUserDto;
	}

	@Override
	public UserDto getUserById(Long id) {
		return modelMapper.map(this.findUserById(id), UserDto.class);
	}

	@Override
	public void deleteUserById(Long id) {
		redisService.deleteKey("user:" + id);
		userRepository.deleteById(id);
	}

	public Users findUserById(Long id) {
		String key = "user:" + id;
		Users cachedJob = redisService.getValue(key, Users.class);
		if (cachedJob != null) {
			return cachedJob;
		}
		Users user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		redisService.setValue(key, user, 3600L);
		return user;
	}

}
