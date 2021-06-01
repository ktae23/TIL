## Angular 7.0 프로그래밍 (2021.06.02)

### 3. Server Classes 작성

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

```jav
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