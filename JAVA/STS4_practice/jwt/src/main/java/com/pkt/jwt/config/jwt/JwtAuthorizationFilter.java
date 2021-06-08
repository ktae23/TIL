package com.pkt.jwt.config.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.pkt.jwt.config.auth.PrincipalDetails;
import com.pkt.jwt.repository.UserRepository;
import com.pkt.jwt.user.User;

// Security가 filter 가지고 있는데 그 필터중에  BasicAuthenticationFilter 라는 것이 있음
public class JwtAuthorizationFilter extends BasicAuthenticationFilter{
	
	private UserRepository userRepository;

	public JwtAuthorizationFilter(AuthenticationManager authenticationManager, UserRepository userRepository) {
		super(authenticationManager);
		this.userRepository = userRepository;
		
	}
	// 권한이나 인증이 필요한 특정 주소를 요청했을 때 위 필터를 무조건 타게 되어 있음
	// 권한이나 인증이 필요한 주소가 아니라면 이 필터를 타지 않음
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		//super.doFilterInternal(request, response, chain);// 지우지 않으면 응답을 2번 탐
		System.out.println("인증이나 권한이 필요한 주소가 요청 됨");
		
		String jwtHeader = request.getHeader("Authorization");
		System.out.println("jwtHeader : " + jwtHeader);
		
		// header 유무 확인 및 검증
		if(jwtHeader == null || !jwtHeader.startsWith("Bearer")) {
			chain.doFilter(request, response);
			return;
		}
		// JWT 검증해서 정상적인 사용자인지 확인
		String jwtToken = request.getHeader("Authorizationi").replace("Bearer ","");
		
		String userName = 
				JWT.require(Algorithm.HMAC256("cos")).build()
				.verify(jwtToken)
				.getClaim("userName")
				.asString();
		// 서명이 정상적일 경우
		if(userName != null) {
			User userEntity = userRepository.findByUserName(userName);
			
			PrincipalDetails principalDetails = new PrincipalDetails(userEntity);
			// 임의로 Authentication 객체를 만들어 줌 ( 인증이 된 상태기 때문에 (첫 번째 인자는 유저정보, 두번째는 비밀번호, 세번째는 권한))
			// JWT 토큰 서명을 통해 서명이 정상이면 Authentication 객체를 생성
			Authentication authentication = 
					new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
		
			// 시큐리티 저장 가능한 세션 공간에 강제로 접근하여 Authentication 객체를 저장.
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			
		}
		chain.doFilter(request, response);
	}

}
