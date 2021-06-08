package com.pkt.jwt.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

	@Bean
	public CorsFilter  corsFilter() {
		CorsConfiguration config = new CorsConfiguration();
		// 내 서버가 응답을 할 때 json을 자바스크립트에서 처리 할 수 있게 할지 설정하는 것
		config.setAllowCredentials(true);
		// 모든 ip에 응답 허용
		config.addAllowedOrigin("*");
		// 모든 header에 응답 허용
		config.addAllowedHeader("*");
		// 모든 메서드(get, post 등)에 응답 허용
		config.addAllowedMethod("*");
		// /api/로 들어오는 모든 요청은 이 설정을 따라라
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return new CorsFilter(source);
	}
	

}