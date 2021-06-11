package com.pkt.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity// 스프링 시큐리티 필터가 스프링 필터체인에 등록
public class securityConfig extends WebSecurityConfigurerAdapter{
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
	http.csrf().disable();
	http.authorizeRequests()
	.antMatchers("/user/**")
		.authenticated()
	.antMatchers("/manager/**")
		.access("hasRole('ROLE_ADMIN')or hasRole('role_manager')")
	.antMatchers("/amdin/**")
		.access("hasRole('ROLE_ADMIN')or hasRole('role_manager')")
	.anyRequest().permitAll();
		
	}
}
