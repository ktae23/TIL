## SpringBoot project 기본 구조 연습

![프로젝트구조](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.04.07/springboot_project.png)



#### application.properties

```properties
#Server
server.port=[포트번호]
server.servlet.session.timeout=3600

#JDBC
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:XE
spring.datasource.username=[스키마이름]
spring.datasource.password=[비밀번호]
spring.datasource.driver-class-name=oracle.jdbc.driver.OracleDriver

#MyBatis
mybatis.config-location=classpath:mybatis/myBatisConfig.xml
mybatis.type-aliases-package=com.proto.mm.DTO

#file size limit
spring.servlet.multipart.maxFileSize=10MB
spring.servlet.multipart.maxRequestSize=10MB

#Thymeleaf
spring.thymeleaf.prefix=classpath:templates/
spring.thymeleaf.check-template-location=true
spring.thymeleaf.suffix=.html
spring.thymeleaf.mode=HTML5
#개발 모드로 수정하면서 확인 할경우 fals, 운영 모드에선 true
spring.thymeleaf.cache=false
spring.thymeleaf.order=0

```

<br/>

#### [프로젝트]Application.java

```java
package com.proto.mm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan
@SpringBootApplication
public class MMProtoTypeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MMProtoTypeApplication.class, args);
	}

}
```

<br/>

#### Controller

```java
package com.proto.mm.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.proto.mm.DTO.MemberDTO;
import com.proto.mm.Service.MemberService;


@Controller
public class MainController{
	@Autowired
	MemberService memberService;
	
	@GetMapping("")
	public String mainView() {
		return "index";
	}

	@GetMapping("test")
	public String getMessage(Model model) {
		model.addAttribute("testSTR","타임리프 연습");
		return "testView";
	}// testView.html 내의 ${testSTR}에 "타임리프 연습"을 전달
	
	@GetMapping("memberList")
	public String member(Model model) {
		List<MemberDTO> memberList = memberService.memberView();
		model.addAttribute("memberList", memberList);
		for(MemberDTO member : memberList) {
			System.out.println(member.toString());
		}
		
		return "memberList";
	}// 멤버 목록을 DB에서 조회하여 전달
	
}

```

<br/>

#### Service

```java
package com.proto.mm.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proto.mm.DAO.MemberDAO;
import com.proto.mm.DTO.MemberDTO;


@Service
public class MemberService{
	
	@Autowired
	MemberDAO memberDAO;

	public List<MemberDTO> memberView() {
		return memberDAO.memberView();
	}

	
}

```

<br/>

#### DTO

```java
package com.proto.mm.DAO;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.proto.mm.DTO.MemberDTO;


@Mapper
@Repository
public interface MemberDAO{
	
	public List<MemberDTO> memberView();

	
}

```

<br/>

#### DTO (feat.Lombok)

```java
package com.proto.mm.DTO;

import lombok.AllArgsConstructor; //모든 인자 포함한 생성자
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor; // 인자 없는 생성자
import lombok.NonNull;
import lombok.RequiredArgsConstructor; // NonNull을 반드시 포함하는 생성자
import lombok.Setter;
import lombok.ToString;

@Data //Data 어노테이션 하나로 위의 모든 어노테이션 대체 가능
public class MemberDTO{

	@NonNull
	private String id; //id는 Nul이 될 수 없다.
    
	private String pw;
	private String name;

}

```

<br/>

#### mappers.member.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    
<mapper namespace="com.proto.mm.DAO.MemberDAO"><!--패키지+클래스 명 전부 적기-->

	
	<select id="memberView" resultType="memberDTO">
		<![CDATA[
			select id,pw,name
			from member
		]]>
	</select>

	
</mapper>
```

<br/>

#### myBatisConfig.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
 
	<mappers>
		<mapper resource="mybatis/mappers/member.xml"/>
	</mappers>	

</configuration>
```

<br/>

#### memberList.html

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
<table border=1>
<thead>
		<tr>
			<th>ID</th>
			<th>PW</th>
			<th>NAME</th>
		</tr>
</thead>
<thead>
 		<tr th:each="member : ${memberList}">
			<td><span th:text="${member.id}"></span></td>
			<td><span th:text="${member.pw}"></span></td>
			<td><span th:text="${member.name}"></span></td>
		</tr>
	
</thead>
</table>

</body>
</html>
```

![DB](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.04.07/memberList.png)

![결과 화면](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.04.07/memberList.png)



[타임리프 참조 블로그](https://sidepower.tistory.com/145)

[롬복 설치 참조 블로그](https://duzi077.tistory.com/142)

* 롬복 반디집 등 압축 풀기로만 나올 경우

![롬복_jar 풀기](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.04.07/Lombok_jar.png)