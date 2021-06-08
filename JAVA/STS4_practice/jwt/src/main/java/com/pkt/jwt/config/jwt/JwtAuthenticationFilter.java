package com.pkt.jwt.config.jwt;

import java.io.IOException;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkt.jwt.config.auth.PrincipalDetails;
import com.pkt.jwt.user.User;

import lombok.RequiredArgsConstructor;

// 스프링 시큐리티에 UsernamePasswordAuthenticationFilter가 있음
// /login 요청해서 Username, Password를 (post) 전송하면 해당 필터가 동작 함

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{
	private final AuthenticationManager authenticationManager;
	
	
	// /login 요청을 하면 로그인 시도를 위해 실행되는 함수
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		System.out.println("JwtAuthenticationFilter : 로그인 시도 중");
		
		// 1. userName, passWord 받기
		try {
//			BufferedReader br = request.getReader();
//			
//			String input = null;
//			while((input = br.readLine()) != null) {
//				System.out.println(input);
//			}
			
			ObjectMapper om = new ObjectMapper();
			User user = om.readValue(request.getInputStream(), User.class);
			System.out.println(user);
			
			UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassWord());
			
			System.out.println("1==================================");
			//PrincipalDetailsService의 loadUserByUserName()함수가 실행된 후 정상이면 authentication이 리턴 됨
			// DB에 있는 userName과 PassWord가 일치 한다.
			Authentication authentication = 
					authenticationManager.authenticate(authenticationToken);
			
			PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
			System.out.println("로그인 완료 됨" + principalDetails.getUser().getUserName());
			// authentication 객체가 session영역에 저장을 해야하는데 그 방법으로 return을 해줌
			// 권한 관리를 security가 대신 해주기 때문에 편하고자 return을 하는 것
			// JWT 토큰을 이용하면 세션을 만들 이유가 없지만 권한처리 때문에 session 넣어서 사용
			return authentication;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("2==================================");
		// 2. 로그인 시도를 통해 정상인지 확인
		// authenticationManager로 시도 시 PrincipalDetails Service가 실행 됨
		// loadUserByUsername() 함수 실행 됨
		// 3. PrincipalDetils를 세션에 담고 (권한 관리를 위해)
		// 4. JWT를 생성해서 응답
		
		return null;
	}
	
	//attemptAuthentication 실행 후 인증이 정상적으로 되었으면 successfulAuthentication 함수가 실행 됨
	// JWT 만들어서 request 한 사용자에게 JWT를 response 해 줌
	 @Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		 System.out.println("successfulAuthentication 실행 됨: 인증이 완료되었습니다.");
		 PrincipalDetails principalDetails = (PrincipalDetails) authResult.getPrincipal();
		 
		 String jwtToken = JWT.create()
				 // 토큰 이름
				 .withSubject(principalDetails.getUsername())//
				 // 만료 시간
				 .withExpiresAt(new Date(System.currentTimeMillis()+JwtProperties.EXPIRATION_TIME))//
				 // 비공개 클레임으로 사용자가 넣고 싶은 값을 입력 함
				 .withClaim("id", principalDetails.getUser().getId())
				 .withClaim("userName", principalDetails.getUser().getUserName())
				 .sign(Algorithm.HMAC512(JwtProperties.SECRET));//
		 
		 // 인증방식 뒤에 한칸 띄어야 함
		 response.addHeader("Authorization", JwtProperties.TOKEN_PREFIX+ jwtToken);
		 
	}
}
