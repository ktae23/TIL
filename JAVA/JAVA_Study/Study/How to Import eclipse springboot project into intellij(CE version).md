## 인텔리제이(CE)에서 이클립스 스프링부트 프로젝트 임포트하는 방법(2021 -03)

> How to Import eclipse springboot project into intellij(CE version)

<br/>

### 1. 스프링 이니셜라이저

- [start.spring.io](https://start.spring.io/)에서 기존 프로젝트와 동일하게 의존성 및 프로젝트 세팅을 하고 GENERATE하여 *.zip 파일로 다운로드

<br/>

### 2. 인텔리제이에서 임포트

- 1번에서 생성한 프로젝트를 압축 해제한 뒤 File - Open - 프로젝트 - OK

<br/>

### 3. 서버 설정하기(포트 변경)

![modify_port](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\modify_port.png)

![modify_port2](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\modify_port2.png)

- Name에 `server.port`
- Value에 포트 번호

<br/>

-  사용 중인 포트 확인 및 포트 종료 방법

```shell
C:\WINDOWS\system32>netstat -a -o

[~~~ 주소 ~~~~~ PID]

// 로컬 포트 주소에 해당하는 pid 번호를 아래 코드에 넣어서 태스크 킬

C:\WINDOWS\system32>taskkill /f /pid 4524
성공: 프로세스(PID 4524)가 종료되었습니다.
```

- 이후 재 빌드

<br/>

#### 4. 기존 프로젝트에서 파일 덮어 쓰기

> 주의 : 아래 두가지 자바 파일이 있어야 스프링부트 실행 가능

<br/>

#### ServletInitializer.java

```java
package sample1;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Sample1Application.class);
	}

}

```

<br/>

#### [프로젝트]Application.java

```java
package sample1;

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

#### Spring Security 주입 시

```java
package config;

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
```

- 위 경로 접속 허용은 필수가 아님, 하지만 시큐어 설정 클래스를 만들어 애너테이션을 달아주어야 함

- 그러면 포트 접속 시 아래와 같은 로그인 창이 열림

<br/>

![springboot_security](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\springboot_security.png)

<br/>

- id에 user
- pw에 스프링부트 구동 시 나오는 비밀번호 입력

![springboot_security_pw](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\springboot_security_pw.png)

<br/>