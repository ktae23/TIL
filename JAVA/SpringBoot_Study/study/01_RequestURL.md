#### ![](https://benefits.fastfive.co.kr/wp-content/uploads/2017/12/logo_%ED%8C%A8%EC%8A%A4%ED%8A%B8%EC%BA%A0%ED%8D%BC%EC%8A%A4-600x500.png)

# 01. chap1. 1.주소요청에 대한 이해_Q1

통계상 인텔리 제이 사용률이 높음

이클립스 매뉴얼이 현업에서 많기에 이클립스로 예제 진행으로 처음 시작

이후는 인텔리 제이 사용

<br/>

##### STS 스프링 툴 슈트 작업하거나 스프링에서 STS 플러그인 설치

```
help -> eclipse Marketplace -> spring 검색 -> Spring Tools 4 설치
```

<br/>

##### 스프링 프로젝트 생성

```
create project -> Spring Boot -> Spring Starter Project
```

<br/>

##### 프로젝트 설정

- 그레이들
- jar
- java 8

<br/>

##### Dependencies

- Spring Boot Version 2.4.2 
  - 나한테는 안떠서 2.4.3 사용

- H2 db
- Lombok
- Spring Configuration Processor
- Spring Data JPA
- Spring REST Docs
- Spring Security
- Spring Web

<br/>

##### 실행 시 기본 생성 자바 파일

```java
package com.example.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Sample1Application {

	public static void main(String[] args) {
		SpringApplication.run(Sample1Application.class, args);
	}

}
```

<br/>

##### 1번 문제 풀이 - 기본 컨트롤러 구현

```java
package com.example.jpa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class FirstController {

	//기본적으로 GET방식 이지만 명시 할 경우 매핑을 value로, 방식을 method로 명시해 줌
	@RequestMapping(value ="/first-url", method = RequestMethod.GET)
	public void first() {
		
	}
	
	
}
```

<br/>

##### Spring Security 사용을 위한 설정 클래스

```
package com.example.jpa;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

//설정 및 보안 접속 가능하도록 애너테이션 작성
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
```

