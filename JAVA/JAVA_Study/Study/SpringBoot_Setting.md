## 스프링 부트

- 애너테이션 기능이 강화되면서 점차 웹 애플리케이션도 일반 응용프로그램을 개발하는 방식으로 바뀌기 시작함

- 이를 가능하게 한 것이 스프링부트

- 일반 응용 프로그램을 단독으로 실ㅇ행하는 수준으로 구현 
- 프로젝트 환경 구축 시 필요한 톰캣, Jetty, UnderFlow 같은 서버 외적인 툴이 내장되어 있어 따로 설치할 필요 없음

- XML 기반 설정이나 코드 없이 환경 설정 자동화 가능
- 의존성 관리를 쉽게 자동으로 할 수 있음

<br/>

![sts_start](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\sts_start.png)

<br/>

##### Type (의존성 관리 도구)

- maven
- gradle

<br/>

##### packging (배포 단위)

- Jar
  - Java Archive
- War
  - Web Archive
    - WEB-INF 를 비롯한 웹 구조 포함

<br/>

##### Group

- 도메인 거꾸로 작성하는 관례

- package 명과 비슷하게 지정되는 편

<br/>

#### Dependencies

![dependencies](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\dependencies.png)

- H2 Database
  - 내부 사용을 위해
- JDBC API
- Spring WEb
- Mybatis
- Oracle Driver

<br/>

#### 프로젝트명 변경 시

```xml
	<groupId>my.pkt</groupId>
	<artifactId>SpringBootStarter1</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>war</packaging>
	<name>SpringBootStarter1</name>
	<description>First project for Spring Boot</description>
```

- pom.xml에서 groupID와 name도 변경 필요
- Class 명 변경 필요

<br/>

> main() 메서드를 시작점으로 실행
>
> ServletInitailizer.java 파일에 생성 된ServletInitialiaer 클래스는 SpringBootServletInitailizer 클래스를 상속 받음
>
> 스프링 부트 애플리케이션을 web.xml 없이 

<br/>

##### Controller 만들기

```java
package my.pkt.web;

import org.springframework.stereotype.Controller;

@Controller
public class HomeController {

   	@ResponseBody
	@RequestMapping("/home")
	public String home() {
		System.out.println("Hello Boot~!");
		return "Hello Boot!";
	}

}
```

- 일반적인 자바 클래스 생성 후 @Controller 애너테이션 입력 + 임포트

<br/>

#### DevTools dependency

```xml
<!-- https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-devtools -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <version>2.4.4</version>
</dependency>
```

- 초기 의존성 주입 할 때 넣을 수 있음

<br/>

#### JSON dependency

```xml
<!-- https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple -->
<dependency>
    <groupId>com.googlecode.json-simple</groupId>
    <artifactId>json-simple</artifactId>
    <version>1.1.1</version>
</dependency>

```

<br/>

### application.properties

```properties
# Server
server.port=[포트 번호]
server.servlet.session.timeout=[세션 유효 시간]

#DataSource
spring.datasource.driver-class-name=oracle.jdbc.driver.OracleDriver
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:XE
spring.datasource.username=[스키마]
spring.datasource.password=[비밀번호]

#MyBatis
mybatis.config-location=classpath:[파일 경로]myBatisConfig.xml
mybatis.type-aliases-package= 패키지명.클래스명
```

- 서버 설정
  - 세션 시간 길면 OOM 위험

> 톰캣에서 실행하게 해주는 역할

<br/>

##### src/main/resources/mybatis/mapper/myBatisConfig.xml

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>

	<mappers>
		<mapper resource="mybatis/mapper/[매퍼설정].xml"/>
		<mapper resource="mybatis/mapper/[매퍼설정2].xml"/>
	</mappers>	

</configuration>

```

<br/>

##### src/main/resources/mybatis/mapper/sample.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    
<mapper namespace="mapper.[매퍼이름]">
</mapper>

```

<br/>

### New - Web 폴더 없을 경우

- JavaEE 가 없는 경우로 Help - Install New Software에서 설치
  - eclipse 검색 후 Web,XML, Java EE ~~ 설치

![STS_J2EE_1](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\STS_J2EE_1.png)

![STS_J2EE_2](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\STS_J2EE_2.png)

![STS_J2EE_3](C:\Users\zz238\TIL\JAVA\JAVA_Study\Study\imgs\STS_J2EE_3.png)