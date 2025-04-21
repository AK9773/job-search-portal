package com.ak.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ak.entity.UserPrincipal;
import com.ak.entity.Users;
import com.ak.repository.UserRepository;

@Service
public class MyUserDetailsService implements UserDetailsService {

	private UserRepository userRepository;

	public MyUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		List<Users> byName = userRepository.findByName(username);
		if (byName.isEmpty()) {
			throw new RuntimeException("No user found with name: " + username);
		}
		Users user = byName.get(0);
		
		return new UserPrincipal(user);

	}

}
