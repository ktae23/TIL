package com.pkt.jwt.config.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.pkt.jwt.repository.UserRepository;
import com.pkt.jwt.user.User;

import lombok.RequiredArgsConstructor;
// httpL//localhost:8080/login - 기본적으로  /login이 스프링 시큐리티의 주소
// 하지만 formLogin을 안하기로 했으니 필터에 직접 넣어주어야 함
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService{
	
	private final UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		System.out.println("PrincipalDetails Service의 메서드 호출");
		User userEntity = userRepository.findByUserName(userName);
		return new PrincipalDetails(userEntity);
	}

}
