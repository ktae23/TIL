## [JWT(JSON Web Token)](https://jwt.io/)

#### [Session](https://www.youtube.com/watch?v=cv6syIv-8eo&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=12)

- Controller에서 Response로 보낼 때, 최초 요청일 경우 Http Header에 Cookie에 세션 ID를 담아서 보냄
  - 브라우저 Cookie 영역에 세션 ID가 저장 됨
  - 두 번째 이상 요청을 보낼 땐 쿠키에 세션 ID를 함께 담아서 전송 함
- 세션이 사라지는 경우
  - 서버에서 세션의 값을 제거
  - 사용자가 브라우저를 종료
  - 션의 만료 시간이 지났을 경우
- 세션의 단점
  - 서버에서 세션을 관리하기 때문에 많은 사용자가 몰릴 경우 부하가 생김

<br/>

#### [TCP](https://www.youtube.com/watch?v=0PCxZ_mIaAo&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=13)

##### 통신 OSI 7계층

1. 응용
2. 프리젠테이션 : 암호화(압축)
3. 세션 : 인증 체크(접근 가능한지 여부 등)
4. 트랜스포트 : TCP/UDP 선택
   1.  TCP : 데이터가 잘 갔는지 여부를 매번 확인
      1. 신뢰성
      2. 느림
   2. UDP : 데이터 전송 확인을 하지 않음
      1. 신뢰 낮음
      2. 빠름
      3. 인터넷 전화, 동영상 전송 등 일부 유실되어도 이용 가능한 서비스에서 주로 사용
5. 네트워크 : IP
6. 데이터링크 : LAN, WAN
7. 물리 : 광케이블

<br/>

전송 층 : 1~7 -> 수신 층 : 7~1

<br/>

#### [CIA(Confidentiality Integrity Availability)](https://www.youtube.com/watch?v=d9huoyT_Z5g&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=14)

- 기밀성(Confidentiality), 무결성(Integrity), 가용성(Availability)
- 암호 키 문제, 데이터 발신자 확인 문제

<br/>

#### [RSA](https://www.youtube.com/watch?v=edyjcg7_Lyc&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=15)

- 공개키(Public Key)
- 개인키(Private Key)
- 상대방의 공개키로 암호화 한 뒤 전송하면 받는 쪽의 개인 키로 복호화 가능 (암호화)
- 반대로 자신의 개인키로 암호화 한 뒤 전송하면 받는 쪽에서 보낸 쪽의 공개키로 복호화 가능 (전자서명)
  - 비대칭키라고 한다.

<br/>

#### [RFC (Request for Comments)](https://www.youtube.com/watch?v=PnqpOgNS40I&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=16)

- [JWT.io](https://jwt.io/)
  - JWT에 대한 설명이 나와 있음
  - [RFC 7519 문서](https://datatracker.ietf.org/doc/html/rfc7519)
    - 각 통신 규칙이 생길 때마다 하나씩 작성, 출판 된 이후 절대 폐지 또는 수정 되지 않는다. [위키백과](https://ko.wikipedia.org/wiki/RFC)
    - **HTTP**(HyperText Transfer Protocol)와 같이 통신 규칙을 정의 한 문서를 의미
    - JWT는 7519번재 RFC 문서

<br/>

#### [JWT 구조](https://www.youtube.com/watch?v=JY6qEnGRXic&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=17)

- JWT의 핵심은 작성자를 인증하는 서명에 있다.

```json
xxxxx.yyyyy.zzzzz
```

##### Header.Payload.Signature

#### * Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg` : `알고리즘 종류`
- `typ` : `타입`
- `Base64Url`로 인코딩 되어 있다.(암호화,복호화 가능)

<br/>

#### * Payload

``` json
{
    "sub" : "1234567890",// 등록된 클레임
    "name" : "Jogh Doe", // 등록된 클레임
    "admin" : true //개인 클레임
}
```

- `Registered claims` : 필수는 아니지만 권장되는 미리 정의 된 클레임 집합
  - `iss` : 발행자
  - `exp` : 만료 시간
  - `sub` : 주제
  - `aud` : 청중
  - ETC
- `Public claims` : JWT를 사용하는 사람들이 원하는 대로 정의 할 수 있음
  - [IANA JSON 웹 토큰 레지스트리](https://www.iana.org/assignments/jwt/jwt.xhtml)에서 정의하거나 충돌 방지 네임 스페이스를 포함하는 URI로 정의 해야 함
- `Private claims` : 사용에 동의하고 등록 또는 공개 클레임이 아닌 당사자간에 정보를 공유하기 위해 생성 된 사용자 지정 클레임

<br/>

#### * Signature

```json
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret)
```

- 작성한 Header, Payload 및 암호화 키를 가져와 헤더에 작성한 알고리즘을 사용하여 서명 

- 메시지 위변조 여부를 인증하는 목적으로 사용되며 개인 키로 서명 된 토큰의 경우 JWT의 발신자를 확인하는 용도로도 사용 됨

<br/>

- 로그인 시도 시 인증 완료 후 서버에서 header, payload, signature를 작성해서 만든 JWT를 응답에 싣어 보낸다.
  - Web Browser - Local Storage에 저장 해 둠
  - 이후 요청을 보낼 때 마다 JWT를 싣어서 보냄
- 서버에서는 요청에 포함 되어 넘어 온 JWT가 서버에 저장된 header, payload, 시크릿 키를 암호화 한 값과 같은지 비교하여 인증

<br/>

- RSA 방식일 경우 시크릿키 없이 개인키, 공개키 비대칭 키 방식으로 사용

<br/>

### 실습

- 실습 환경
  - Spring Starter Project
  - dependency
    - Lombok
    - Spring Boot DevTools
    - Spring Web
    - Spring Data JPA
    - MySQL Driver
    - Spring Security
- maven repository [Java JWT](https://mvnrepository.com/artifact/com.auth0/java-jwt)

```xml
<!-- https://mvnrepository.com/artifact/com.auth0/java-jwt -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>3.15.0</version>
</dependency>
```

<br/>

#### JWT

```java
String lowSig = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
	+"eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ"
    +"시크릿 키"
===================
JWT
BASE64(헤더).
BASE64(페이로드).
BASE64(HS256암호화(lowSig))
```

- JWT 라이브러리가 이러한 작업을 자동화 해줌

<br/>

##### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
      
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/security?serverTimezone=Asia/Seoul
    username: cos
    password: cos1234

  jpa:
    hibernate:
      ddl-auto: create #create update none
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
    show-sql: true
    database: mysql
```

<br/>

##### User.java

```java
package com.pkt.jwt.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String userName;
	private String passWord;
	private String roles; //USER, ADMIN
	
	public List<String> getRoleList(){
		if(this.roles.length() > 0) {
			return Arrays.asList(this.roles.split(","));
		}
		return new ArrayList<>();
	}
}

```



<br/>

##### config/SecurityConfig.java

```java
package com.pkt.jwt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.pkt.jwt.filter.MyFilter3;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private CorsConfig corsConfig;

	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		/* BasicAuthenticationFilter 실행되기 전에 필터가 작동되도록
		 Config로 따로 관리 가능, 하지만 시큐리티 필터가 먼저 작동
		 시큐리티보다 먼저 작동하길 원하면 필터 비포에 SecurityContextpersistenceFilter.class를 걸어준다*/
		.addFilterBefore(new MyFilter3(), BasicAuthenticationFilter.class )
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

```

<Br/>

##### config/CorsConfig.java

```java
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
```

<br/>

### JWT Bearer 인증 방식

##### Http Basic 방식

- Header에 Authorization에 Id, Pw를 넣어서 보내는 방식
  - Http Basic 방식
  - 쿠키 사용하지 않고 확장성 좋음
  - 하지만 보안에 취약
    - 이를 보완하기 위해 https 를 사용

<br/>

##### Bearer 방식

- Authorization에 토큰(JWT)을 담아서 보냄
  - Id, Pw를 기반으로 암호화하고 만료 시간 이후 재 발급하기 때문에 노출이 돼도 다른 방식에 비해 위험이 적음
- 이 방법을 쓰겠다는 선언부

```java
		// 세션을 사용하지 않겠다.
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()
	// 폼을 이용한 로그인 하지 않겠다.
		.formLogin().disable()
		// 기본적인 http 방식 사용하지 않겠다.
		.httpBasic().disable()
		.authorizeRequests()
```

<br/>

##### filter/Myfilter.java

```java
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
		
		
		// 토믄 : 코스(임시)
		// 시큐리티보다 먼저 실행되어야 함
		// ID, PW 정상적으로 들어와서 로그인이 완료 되면 토큰을 생성하고 응답을 해줌
		// 요청을 할때마다 header에 Authorization에 value로 토큰을 담음
		// 그때 넘어오는 토큰이 내가 만든 토큰이 맞는지 검증만 하면 됨 (RSA or HS256)
		if(req.getMethod().equals("POST")) {
			System.out.println("포스트 요청 됨");
			String headerAuth = req.getHeader("Authorization");
			System.out.println(headerAuth);
			System.out.println("필터1");
			if(headerAuth.equals("cos")) {
				// 필터를 체인 걸어야 1회성이 아님
				chain.doFilter(req, res);
			}else {
				
				PrintWriter out = res.getWriter();
				out.println("인증 안됨");
			}
		}
		


	}

}

```

