package com.pkt.jwt.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;


public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		// CSRF (Cross-site request forgery) 사이트 간 요청 위조
		http.csrf().disable();
		// 세션을 사용하지 않겠다.
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
		// 폼을 이용한 로그인 하지 않겠다.
		.formLogin().disable()
		.httpBasic().disable()
		.authorizeRequests()
		// 이런 주소로 매핑이 되면
		.antMatchers("/api/v1/user/**")
		.access("hasRole('ROLE_USER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")"
		
	}
}
