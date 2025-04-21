package com.ak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ak.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long> {

	List<Users> findByName(String username);

}
