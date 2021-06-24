package com.pkt.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Auth {

	@RequestMapping
	public String index() {
		return"홈페이지";
	}
	
	@RequestMapping("/auth")
	public Authentication auth(){
	    return SecurityContextHolder.getContext()
	        .getAuthentication();
	}
	
	// USER 권환이 있어야만 접근 가능
	@PreAuthorize("hasAnyAuthority('ROLE_USER')")
	@RequestMapping("/user")
	public SecurityMessage user() {
		return SecurityMessage.builder()
				.auth(SecurityContextHolder.getContext().getAuthentication())
				.message("User 정보")
				.build();
	}
	
	// ADMIN 권한이 있어야만 접근 가능
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
	@RequestMapping("/admin")
	public SecurityMessage admin() {
		return SecurityMessage.builder()
				.auth(SecurityContextHolder.getContext().getAuthentication())
				.message("관리자 정보")
				.build();
		
	}
}
