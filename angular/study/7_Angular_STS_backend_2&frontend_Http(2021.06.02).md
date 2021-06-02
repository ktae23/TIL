## Angular 7.0 프로그래밍 (2021.06.02)

### Server Classes 작성

##### HeroRestController class (Controller)

```java
package com.angular.serverSide.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.angular.serverSide.entity.Hero;
import com.angular.serverSide.repository.HeroRepository;

// PUT, DELETE는 기본적으로 막혀 있기 때문에 따로 허용 설정해야 함.
// @CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping(value = "/heroes")
public class HeroRestController {
	//Logger 생성
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	// Setter Injection
	// @Autowired
	// private HeroRepository heroRepository;

	// Constructor Injection
	// final 선언을 해서 컴파일 단계에서 에러 잡아낼 수 있음
	private final HeroRepository heroRepository;

	public HeroRestController(HeroRepository repository) {
		this.heroRepository = repository;
	}

}

```

- @Controller
  - 서버사이드 렌더링(Thymeleaf)
- @RestController
  - 클라이언트사이드 렌더링(JSON, XML)
  - @Controller + @ResponseBody
    - @ResponseBody 는 변환 된 JSON을 Reponse의 Body에 추가해주는 역할
  - JSON 반환을 위해 사용
    - Jackson 라이브러리가 자바 객체를  JSON으로 변환 해줌
  - @RequestBody
    - Jackson 라이브러리가 JSON을 Java로 변환 해줌
    - 변환된 Java 객체를 컨트롤러내에 있는 메서드의 인자로 매핑해주는 역할

<br/>

##### HeroRestController -> HeroRepository_주입 방식

- Setter Injection : Setter, 변수	

```java
// Setter Injection
@Autowired
private HeroRepository heroRepository;
```

- Constructor Injection : 생성자

```java
// Constructor Injection
// final 선언을 해서 컴파일 단계에서 에러 잡아낼 수 있음
private final HeroRepository heroRepository;

public HeroRestController(HeroRepository repository) {
		this.heroRepository = repository;
	}
```

<br/>

##### Logger

- Loging Facade : Interface
  - SLF4J 
- Loggin 구현체
  - Log4j
  - LogBack 

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Logger 생성
private final Logger logger = LoggerFactory.getLogger(this.getClass());
```

<br/>

##### Runner

- 어플리케이션 동작과 동시에 리스트 내의 데이터 H2 데이터베이스로 저장되도록 설정

```java
package com.angular.serverSide.runner;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.angular.serverSide.entity.Hero;
import com.angular.serverSide.repository.HeroRepository;

@Component
public class DataInitRunner implements ApplicationRunner{
	
	@Autowired
	private HeroRepository repository;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		List<String> nameList = List.of(
				 "Mr.Nice",
				 "Narco",
				 "Bombasto",
				 "Celeritas",
				 "Magneta",
				 "RubberMan",
				 "Dynama",
				 "Dr IQ",
				 "Magma",
				 "Tornado"
				);
		
		nameList.forEach(name -> {
			Hero hero = new Hero();
			hero.setName(name);
			repository.save(name);
		});
	}
}

```

- 외부 DB 사용 시는 Application.properties에 `spring.jpa.hibernate.ddl-auto=create` 옵션을 줘야 함
- 또한 Entity 클래스에 기본 생성자가 필요

<br/>

##### Ajax는 SOP(Single Origin Policy) 원칙

- Origin =`https://domain:port`
  - Client : `http:localhost/4200`
  - Server : `http:localhost/8080`
    - CORS (Cross Origin Resource Sharing)가 필요
- SpringBoot에서 CORS 설정 방법
  - @CrossOrign 애너테이션 사용
    - Controller 위에 모두 선언해야 하는 단점
    - 개발 모드에서만 필요, 운영 시엔 같은 서버에서 운영하기 때문
  - Configuration Class 작성(전역적인 방벙)
    - WebMvcConfigurer Interface 구현
    - addCorsMapping() 메서드 오버라이딩
- 설정 클래스 만드는것을 권장

##### Config Class 만들 경우

```java
package com.angular.serverSide.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//개발 모드에서만 적용 됨
@Profile("dev")
@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Override
   	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
            	// PUT, DELETE는 기본적으로 막혀 있기 때문에 허용을 설정해야 함.
				.allowedMethods("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
				.allowedOrigins("http://localhost:4200");
	}
   	 
}
```

<br/>

##### spring.profiles.active=

```properties
#개발모드 - dev, 운영모드 - prod
spring.profiles.active=dev
#spring.profiles.active=prod
```

- jar 실행 시 `java -jar --spring.profiles.active=prod xxx.jar` 옵션을 통해 설정을 변경해서 실행 할 수 있음
- `application-{profile}.properties` 형식에 따라 위처럼 모드를 준 다음
  - `src/main/resources` 경로 아래에 `application-dev.properties` or `application-prod.properties`처럼 구분해서 사용 가능
  - Log 단계를 다르게 한다거나 CORS 설정을 하는 등의 설정 구분 가능

<br/>

___

<br/>

### Client Side - Http

##### app.module.ts

```typescript
import { HttpClientModule } from '@angular/common/http';

...
  imports: [
    ...
    HttpClientModule
  ],
...
```

<br/>

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { Observable, of } from 'rxjs';
import {Hero} from './hero';
import { MessageService } from './message.service';
import {HEROES} from './mock-heroes';

@Injectable({
  providedIn: 'root'
})

export class HeroService {

    //HttpClient 주입 받기
  constructor(private http: HttpClient,private messageService: MessageService) { }
    
   // 로그 
  /** Log a HeroService message with the MessageService */
  private log(message: string) {
  this.messageService.add('HeroService: ' + message);
  }
	// URL 선언
  private heroesUrl = 'http://localhost:8080/heroes'; // URL to web api

    // of()로 가상 값을 반환 하는 것이 아닌, API 응답 값을 반환
  getHeroes (): Observable<Hero[]> {
    return this.http.get<Hero[]>(this.heroesUrl);
    }

  getHero(id: number): Observable<Hero> {
    // Todo: send the message _after_ fetching the hero
    const hero = HEROES.find(hero => hero.id === id)!;
    this.messageService.add(`HeroService: fetched hero id=${id}`);
    return of(hero);
    }
}
```

- HttpClient 주입 받기
- HeroService를 수정 : getHeroes() 메서드 수정 –> HttpClient.get() 메서드 사용
  - 수정하기 전의 HeroService.getHeroes()는 RxJS of() 함수를 사용 하였으나, 수정한 후의 getHeroes()에서는 HttpClient의 get() 함수를 사용 
  - HttpClient.get()은 untyped JSON 객체로 response body를 반환합니다. 
    - JSON 데이터의 shape는 서버의 데이터 API에 의해 결정됩니다. 
    - Tour of Heroes 데이터 API는 hero data를 배열로 반환합니다. (Observable의 subscribe() 메서드 사용하기)

<br/>

##### handleError 메서드 선언

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {Hero} from './hero';
import { MessageService } from './message.service';
import {HEROES} from './mock-heroes';
...

  private heroesUrl = 'http://localhost:8080/heroes'; // URL to web api

  getHeroes (): Observable<Hero[]> {
    return this.http.get<Hero[]>(this.heroesUrl)
      .pipe(
      tap(_ => this.log(`fetched heroes`)),
      catchError(this.handleError<Hero[]>('getHeroes', []))
      );
    }

...

  /**
  * Handle Http operation that failed.
  * @param operation - name of the operation that failed,
  * @param result - optional value to return as the
  observable result
  */
  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
    // TODO: send the error to remote logging infrastructure
    console.error(error); // log to console instead
    // TODO: better job of transforming error for user consumption
    this.log(`${operation} failed: ${error.message}`);
    // Let the app keep running by returning an empty result.
    return of(result as T);
    };
  }

}
```

- error를 직접 처리하는 대신 fail된 operation의 name과 return 값으로 구성된 catchError에 오류 처리기 함수를 반환

<br/>

##### hero.service.getHeroes()에 코드 추가

```typescript
.pipe(
    tap(_ => this.log(`fetched heroes`)),
    catchError(this.handleError<Hero[]>('getHeroes', []))
);
```

- pipe() 
  - 여러 개의 순수 함수들을 연결하고 쉽게 공유 논리를 다시 사용
  - 더욱 재사용 가능한 RxJS 코드를 작성할 수 있도록 해줌
- tap()
  - pipe 라인의 메시지에 대해 임의의 작업을 실행하기 위해 사용
  - message stream을 가로 채서 쿼리 동작을 디버깅, 로깅하는 데 사용
