package com.pkt.security.auth;

import org.springframework.security.core.Authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SecurityMessage {
	
	private Authentication auth;
	private String message;

}
