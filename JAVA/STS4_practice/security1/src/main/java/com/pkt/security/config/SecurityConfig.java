package com.pkt.security.config;

import org.springframework.boot.autoconfigure.security.reactive.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


// 웹 시큐리티 설정을 활성화 한다
@EnableWebSecurity(debug = true)
// 프리 포스트(요청에 대해 선제적으로) 권한 체크 하겠다
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter{

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 인증관리자 빌더에 사용자 추가하기
		auth.inMemoryAuthentication()
				.withUser(User.builder()
						.username("user2")
						.password(passwordEncoder().encode("2222"))
						.roles("USER")
				).withUser(User.builder()
						.username("admin")
						.password(passwordEncoder().encode("3333"))
						.roles("ADMIN")
						)
				;
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
        // http 방식의 모든 요청에 대해 인증 받은 상태로 접근해야 한다.
		http.authorizeRequests((requests) ->
				requests
					// "/"에 대한 요청은 모두에게 허용
					.antMatchers("/").permitAll()
					.anyRequest().authenticated()
				
				);
        // form 로그인 사용
		http.formLogin();
        // 기본 http 방식 사용
		http.httpBasic();
		
		
	
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web
			.ignoring()
				.requestMatchers(
						PathRequest.toStaticResources().atCommonLocations()
				)
				;
	}
	
	
}