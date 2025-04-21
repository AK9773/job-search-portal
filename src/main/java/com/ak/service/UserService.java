package com.ak.service;

import com.ak.dto.CreateUserDto;
import com.ak.dto.UpdateUserDto;
import com.ak.dto.UserDto;
import com.ak.entity.Users;

public interface UserService {

	public UserDto insertUser(CreateUserDto user);

	public UserDto getUserById(Long id);

	public UserDto updateUser(long userId, UpdateUserDto user);

	public void deleteUserById(Long id);

	public Users findUserById(Long id);

}
