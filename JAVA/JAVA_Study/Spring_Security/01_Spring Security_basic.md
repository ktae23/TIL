## Spring Security Basic

##### application.yml

```xml
spring:
  security:
    user:
      name: user1
      password: 1111
      roles: USER

```

- 위 설정을 해두면 스프링 시큐리티 작동 시 해당 이름과 비밀번호로 어플리케이션 접근 가능

##### auth controller

```java
package com.pkt.security.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Auth {

	@RequestMapping
	public String index() {
		return"홈페이지";
	}
	
	@RequestMapping("/auth")
	public Authentication auth(){
	    return SecurityContextHolder.getContext()
	        .getAuthentication();
	}
	
	// USER 권환이 있어야만 접근 가능
	@PreAuthorize("hasAnyAuthority('ROLE_USER')")
	@RequestMapping("/user")
	public SecurityMessage user() {
		return SecurityMessage.builder()
				.auth(SecurityContextHolder.getContext().getAuthentication())
				.message("User 정보")
				.build();
	}
	
	// ADMIN 권한이 있어야만 접근 가능
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
	@RequestMapping("/admin")
	public SecurityMessage admin() {
		return SecurityMessage.builder()
				.auth(SecurityContextHolder.getContext().getAuthentication())
				.message("관리자 정보")
				.build();
		
	}
}

```

<br />

##### `/auth` 경로로 접근 시 사용자 인증 정보를 반환

```json
{
  "authorities": [
    {
      "authority": "ROLE_USER"
    }
  ],
  "details": {
    "remoteAddress": "0:0:0:0:0:0:0:1",
    "sessionId": "E8AE2443AABB87DF5A6F14534841AD90"
  },
  "authenticated": true,
  "principal": {
    "password": null,
    "username": "user1",
    "authorities": [
      {
        "authority": "ROLE_USER"
      }
    ],
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "enabled": true
  },
  "credentials": null,
  "name": "user1"
}
```

<br />

##### `/user`경로 접근 시

```json
{
  "auth": {
    "authorities": [
      {
        "authority": "ROLE_USER"
      }
    ],
    "details": {
      "remoteAddress": "0:0:0:0:0:0:0:1",
      "sessionId": "AA8C6C381F0028222DC7C2933DFD0BF4"
    },
    "authenticated": true,
    "principal": {
      "password": null,
      "username": "user1",
      "authorities": [
        {
          "authority": "ROLE_USER"
        }
      ],
      "accountNonExpired": true,
      "accountNonLocked": true,
      "credentialsNonExpired": true,
      "enabled": true
    },
    "credentials": null,
    "name": "user1"
  },
  "message": "User 정보"
}
```

<br />

##### `/admin`경로 접근 시

```json
{
  "auth": {
    "authorities": [
      {
        "authority": "ROLE_USER"
      }
    ],
    "details": {
      "remoteAddress": "0:0:0:0:0:0:0:1",
      "sessionId": "AA8C6C381F0028222DC7C2933DFD0BF4"
    },
    "authenticated": true,
    "principal": {
      "password": null,
      "username": "user1",
      "authorities": [
        {
          "authority": "ROLE_USER"
        }
      ],
      "accountNonExpired": true,
      "accountNonLocked": true,
      "credentialsNonExpired": true,
      "enabled": true
    },
    "credentials": null,
    "name": "user1"
  },
  "message": "관리자 정보"
}
```

<br />

#### 따로 권한 설정을 하지 않을 경우 이처럼 User 권한의 사용자가 관리자 정보를 탈취할 수 있음

```java
package com.pkt.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
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
}
```

- 이 처럼 간단한 권한 체크 활성화만으로도 url 접근을 차단 할 수 있다. 
- 사용자 권한의 경우 yml에는 하나의 정보만 입력 할 수 있기 때문에  `SecurityConfig` 클래스에서 추가 한다.
  - 하지만 별도로 등록 할 경우 yml에 작성한 사용자 정보는 효력을 잃는다.
- 비밀번호를 암호화하지 않을 경우 오류가 나기 때문에 `PasswordEncoder`를 `bean`으로 등록 한다.

<br />

##### 권한 없는 url로 접근 시

```
Whitelabel Error Page
This application has no explicit mapping for /error, so you are seeing this as a fallback.

Sat Jun 19 16:33:24 KST 2021
There was an unexpected error (type=Forbidden, status=403).
```

<br/>

##### 권한 체크 없이 접근하고 싶을 경우 `.antMatchers("/").permitAll()`를 추가하여 `"/"`에 대한 요청은 모두에게 허용하도록 설정

```java
	@Override
	protected void configure(HttpSecurity http) throws Exception {
        // http 방식의 모든 요청에 대해 인증 받은 상태로 접근해야 한다.
		http.authorizeRequests((requests) ->
				requests
                    // "/"에 대한 요청은 모두에게 허용
					.antMatchers("/").permitAll()
					.anyRequest().authenticated()
				
				);
        //
	
	}
```

<br />