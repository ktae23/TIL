package com.example.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter{

	//HTTP 접속에 대한 보안 설정 메서드 오버라이드
	@Override
	protected void configure(HttpSecurity http) throws Exception {

		// 모든 경로의 어떤 접속에 대해서도 허용 하겠다는 설정
		http.authorizeRequests().anyRequest().permitAll();
		}
}
