package com.pkt.jwt.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pkt.jwt.repository.UserRepository;
import com.pkt.jwt.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RestApiController {
	
	private final UserRepository userRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	
	@GetMapping("home")
	public String home() {
		return "<h1>home</h1>";
	}
	@PostMapping("token")
	public String token() {
		return "<h1>token</h1>";
	}
	
	@PostMapping("join")
	public String join(@RequestBody User user) {
		user.setPassWord(bCryptPasswordEncoder.encode(user.getPassWord()));
		user.setRoles("ROLE_USER");
		userRepository.save(user);
		
		return "회원가입 완료";
	}
	
	// user, manager, admin 모두 접근 가능
	@GetMapping("api/v1/user")
	public String user() {
		return "user";
	}
	
	// manager와 admin접근 가능
	@GetMapping("api/v1/manager")
	public String manager() {
		return "manager";
	}
	
	// admin만 접근 가능
	@GetMapping("api/v1/amdin")
	public String amdin() {
		return "amdin";
	}
	
}
