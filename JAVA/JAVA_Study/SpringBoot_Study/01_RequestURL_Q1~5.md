#### ![](https://benefits.fastfive.co.kr/wp-content/uploads/2017/12/logo_%ED%8C%A8%EC%8A%A4%ED%8A%B8%EC%BA%A0%ED%8D%BC%EC%8A%A4-600x500.png)

# 01. chap1. 1.주소요청에 대한 이해

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

##### <br/>

##### 1번 문제 풀이 - 컨트롤러 구현

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

##### 2번 문제 풀이 - 컨트롤러 구현

```java
package com.example.jpa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FirstController {
	
//	@RequestMapping(value="/helloworld", method= RequestMethod.GET)
//  value 와 method가 GET인 것이 디폴트
//  @ResponseBody 애너테이션을 사용해 뷰페이지가 아닌 문자열 리턴 되도록 
	
	@RequestMapping("/helloworld")
	@ResponseBody
	public String helloworld() {
		return "hello world";
	}

}
```

<br/>

##### 3번 문제 풀이 - 컨트롤러 구현

```java
package com.example.jpa;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecondController {

	// @ResponseBody를 쓰지 않는 방법으로 RestController(리턴이 뷰 리졸버가 아닌 스트링)를 사용
	@RequestMapping(value = "/hello-spring", method = RequestMethod.GET)
	public String helloSpring() {
		return "hello spring";
	}

}
```

<br/>

##### 4번 문제 풀이 - 컨트롤러 구현

```java
package com.example.jpa;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecondController {

	// Rest 형식으로 GET을 받는 애너테이션
	// @RequestMapping(value = "/hello-rest", method = RequestMethod.GET)
	// 위 애너테이션과 같은 기능을 하지만 아래가 더 명확 함
	@GetMapping("/hello-rest")
	public String helloRest() {
		return "hello rest";
	}

}
```



<br/>

##### 5번 문제 풀이 - 컨트롤러 구현

```java
package com.example.jpa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecondController {

	@GetMapping("/api/helloworld")
	public String helloRestApi() {
		return "hello rest api";
	}

}

```

