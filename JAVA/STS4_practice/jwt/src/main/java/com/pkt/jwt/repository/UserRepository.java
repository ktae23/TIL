package com.pkt.jwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pkt.jwt.user.User;

public interface UserRepository extends JpaRepository<User, Long>{
	
	public User findByUserName(String userName);

}
