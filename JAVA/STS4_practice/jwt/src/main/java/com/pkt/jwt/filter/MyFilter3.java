package com.pkt.jwt.filter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MyFilter3 implements Filter{

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		
		
		// 토큰 : 코스(임시)
		// 시큐리티보다 먼저 실행되어야 함
		// ID, PW 정상적으로 들어와서 로그인이 완료 되면 토큰을 생성하고 응답을 해줌
		// 요청을 할때마다 header에 Authorization에 value로 토큰을 담음
		// 그때 넘어오는 토큰이 내가 만든 토큰이 맞는지 검증만 하면 됨 (RSA or HS256)
		if(req.getMethod().equals("POST")) {
			System.out.println("포스트 요청 됨");
			String headerAuth = req.getHeader("Authorization");
			System.out.println(headerAuth);
			System.out.println("필터3");
			if(headerAuth != null) {
				if(headerAuth.equals("cos")) {
					// 필터를 체인 걸어야 1회성이 아님
					chain.doFilter(req, res);
				}
			}else {
				PrintWriter out = res.getWriter();
				out.println("인증 안됨");

			}
		}
		


	}

}
