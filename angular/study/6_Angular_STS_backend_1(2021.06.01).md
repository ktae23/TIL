## Angular 7.0 프로그래밍 (2021.06.01)

### 1. STS Setting

##### [Spring initializr](https://start.spring.io/)

- Project
  - maven
- Language
  - java
- Spring Boot version
  - 2.3.11
- Project MetaData
  - Group : com.angular
  - Artifact : server
  - packaging : jar
  - java : 11

- Dependencies
  - Spring Web
  - Spring Boot DevTools
  - H2 Database
  - Spring Data JPA
  - Lombok

<br/>

##### H2 Console

- `localhost:8080/h2-consle` url 입력
  - Spring Boot 가동 시 콘솔에 찍힌 아래 내용에서 따옴표 안쪽 내용을 JDBC URL에 입력 후 Connect  눌러서 연결

```shell
H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:[일련번호]'
```

<br/>

___

<br/>

### 2. Spring Boot

##### @SpringBootApplication

- @EnableAutoConfiguration + @ComponentScan

##### @ComponentScan

- Spring에서는 \<contest:conponent-scan basepackages="">로 설정 했음
  - Bean을 생성하고 관리해주는 설정
  - Spring Boot에서는 @SpringBootApplication가 붙어 있는 클래스의 패키지가 basepackage로 설정 되어 있기 때문에 하위 패키지로 만들어서 관리 해야 함

##### @EnableAutoConfiguration

- Boot에서는 XML이 없다

- @Configuration을 붙인 설정 역할을하는 java 클래스를 이용

- Java Config로 설정 클래스를 자동으로 활성화 시켜주는 역할

- maven-dependency -> spring-bo-autoconfigureXX.jar -> META-INF - >sprinfg.factories -> (143번째 줄 쯤에) WebMvcAutoConfiguration ->ctrl+shift+t(파일 부분 열기)

<br/>

___

<br/>

### 3. Server Classes 작성 (2021.06.02 이어서 진행)

##### Hero class (Entity)

```java
package com.angular.serverSide.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class Hero {
	
	@Id 
    // Auto가 기본, DB에 맞게 시퀀스를 생성해줌
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	public Hero() {
	}

}
```

<br/>

##### HeroRepository interface (Repository)

```java
package com.angular.serverSide.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angular.serverSide.entity.Hero;

//JpaRepository<EntityClassNAme, IdDataType>
public interface HeroRepository extends JpaRepository<Hero, Long>{
	
}

```

- 따로 구현하지 않아도 CRUDRepository에 있는 기본 메소드 제공 됨
  - save(S)
  - saveAll(Iterable<S>)
  - findById(ID)
  - existsById(ID)
  - findAll()
  - findAllById(Iterable<ID>)
  - count()
  - deleteById(ID)
  - delete(T)
  - deleteAll(Iterable<? extends T>)
  - deleteAll()

<br/>

##### HeroRestController class (Controller)

```java
package com.angular.serverSide.controller;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeroRestController {

}
```

- @Controller
  - 서버사이드 렌더링 이용시 viewName등을 위해 사용
- @RestController
  - @Controller + @ResponseBody
    - @ResponseBody 는 변환 된 JSON을 Reponse의 Body에 추가해주는 역할
  - JSON 반환을 위해 사용
    - Jackson 라이브러리가 자바 객체를  JSON으로 변환 해줌

<br/>