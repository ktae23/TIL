package com.pkt.jwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.pkt.jwt.config.jwt.JwtAuthenticationFilter;
import com.pkt.jwt.config.jwt.JwtAuthorizationFilter;
import com.pkt.jwt.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Autowired
	private CorsConfig corsConfig;
	
	@Autowired
	private UserRepository userRepository;

	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		/* BasicAuthenticationFilter 실행되기 전에 필터가 작동되도록
		 Config로 따로 관리 가능, 하지만 시큐리티 필터가 먼저 작동
		 시큐리티보다 먼저 작동하길 원하면 필터비포에 SecurityContextpersistenceFilter.class를 걸어준다*/
//		.addFilterBefore(new MyFilter3(), BasicAuthenticationFilter.class )
		// 서버로 들어오는 모든 요청은 corsFilter를 거치게 된다
		//@Crossorigin(인증X), 시큐리티 필터에 등록 인증(O)
		.addFilter(corsConfig.corsFilter())	
		// CSRF (Cross-site request forgery) 사이트 간 요청 위조
		.csrf().disable();
		// 세션을 사용하지 않겠다.
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
	// 폼을 이용한 로그인 하지 않겠다.
		.formLogin().disable()
		// 기본적인 hhtp 방식 사용하지 않겠다.
		.httpBasic().disable()
		// /login 필터 활성화 필터 주입
		// AuthenticationManager를 통해 로그인을 진행
		.addFilter(new JwtAuthenticationFilter(authenticationManager()))
		.addFilter(new JwtAuthorizationFilter(authenticationManager(), userRepository))
		.authorizeRequests()
		// 이런 주소로 매핑이 되면
		.antMatchers("/api/v1/user/**")
		// 이런 권한을 가진 애들만 통과 가능하고
			.access("hasRole('ROLE_USER') or hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
		.antMatchers("/api/v1/manager/**")
			.access("hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
		.antMatchers("/api/v1/admin/**")
			.access("hasRole('ROLE_ADMIN')")
		// 이 외 모든 요청은 모두 접근 가능
		.anyRequest().permitAll();
		
	}
	
}
