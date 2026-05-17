package com.OnRoot.onroot.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.OnRoot.onroot.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	java.util.Optional<User> findByEmail(String email);
}
