## Angular 7.0 프로그래밍 (2021.06.02)

### REST_ServerSide

##### HeroRestController.java

```java
//@CrossOrigin(origins = "http://localhost:4200")
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

-		Optional은 자바 8부터 추가 된 타입으로 NULL일 경우를 포함하는 타입
   -		isPresent에 Boolean으로 값의 여부가 저장 됨
   -		값이 존재 할 경우 get을 이용해 값을 가져올 수 있음
   -		orElseThrow(Supplier<? extends X>는 값이 있으면 반환, 없으면 Exception을 발생
-		@FunctionalInterface
   -		abstract method를 딱 1개만 가지고 있는 인터페이스
      -		오버라이딩 할 때 람다식으로 표현 가능
   -		Supplier는 FunctionalInterface로 T를 반환하는 get() 메서드 하나 있음

<br/>

___

<br/>

### REST_Client Side

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import {Hero} from './hero';
import { MessageService } from './message.service';
import {HEROES} from './mock-heroes';
import {environment} from '../environments/environment'
@Injectable({
  providedIn: 'root'
})

export class HeroService {
  private heroesUrl:string;

  constructor(private http: HttpClient,private messageService: MessageService) {
    this.heroesUrl = environment.serverUrl;
  }
  /** Log a HeroService message with the MessageService */
  private log(message: string) {
  this.messageService.add('HeroService: ' + message);
  }

  //'http://localhost:8080/heroes'; // URL to web api

	...method...

  
  /*Handle Http operation that failed.*/
  @param operation/* - name of the operation that failed,*/
  @param result/* - optional value to return as the*/
  /*observable result*/
  
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

<br/>

### Get

##### heroes.component.ts

```typescript
getHeroes() : void {
    // this.heroes = this.heroService.getHeroes();
    this.heroService.getHeroes().subscribe(heroesObj => this.heroes = heroesObj);
}
```

- getHeroes 함수 작성

<br/>

##### hero-detail.component.ts

```typescript
getHero(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
this.heroService.getHero(id)
    .subscribe(hero => this.hero = hero);
}
```

- getHero 함수 작성

<br/>

#### ServerSide

##### HeroRestController.java

````java
@GetMapping
public List<Hero> searchHeroes (){
    logger.info("Hero 전체 목록 조회");
    return heroRepository.findAll();
}

@GetMapping("/{id}") // 사용하는 변수명이 다를 경우 @PathVariable("id")로 명시
public Hero getHero(@PathVariable Long id) {
    Optional<Hero> optional = heroRepository.findById(id);
    // 값이 있으면 hero 반환, 업으면 예외처리 발생
    Hero hero = optional.orElseThrow(
        // 직접 구현한 예외처리 클래스 생성자(resourceName, FieldName, FieldValue)
        () -> new ResourceNotFoundException("Hero", "id", id));
    return hero;


    //		return heroRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hero", "id", id));

}
````

<br/>

##### 개발, 운영 모드에 따라 url 값 따로 관리하기

```typescript
// environments/environment.ts
export const environment = {
  production: false,
  serverUrl : 'http://localhost:8080/heroes',
  titel:'Tour of Heroes (Dev)'
};

// environmetns/environment.prod.ts
export const environment = {
  production: true,
  serverUrl : 'http://[domain]:[port]/heroes',
  titel:'Tour of Heroes (Prod)'
};

// hero.service.ts
import {environment} from '../environments/environment'
...
private heroesUrl = environment.serverUrl;
```

- 빌드 후 배포 운영 시엔 environment.prod.ts가 읽힘
- `ng build`로 빌드
  - dist 디렉토리 생성 됨
    - dist - projectName - 하위모든 파일을 SpringBoot Project의 static에 넣어 줌

<br/>

___

<br/>

### Put

##### hero.service.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
  /** PUT: update the hero on the server */
  updateHero (hero: Hero): Observable<Hero> {
    const url = `${this.heroesUrl}/${hero.id}`;
    return this.http.put<Hero>(url, hero, this.httpOptions)
    .pipe(
      tap((hero: Hero) => this.log(`updated hero id=${hero.id}`)),
      catchError(this.handleError<Hero>('updateHero'))
    );
  }
}
```

- Http Header 옵션 설정
- PUT 함수 작성

##### hero-detail.component.ts

```typescript
save(): void {
    if (this.hero) {
        this.heroService.updateHero(this.hero)
            .subscribe(() => this.goBack());
    }
}
```

- save 함수 추가

<br/>

### ServerSide

##### HeroRestController.java

````java
@PutMapping("/{id}")
	public Hero updateHero(@PathVariable Long id, @RequestBody Hero heroDetail) {
		logger.info("Hero 수정 " + heroDetail);
		
		Hero hero = heroRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Hero","id", id));
		hero.setName(heroDetail.getName());
		
		Hero updatedHero = heroRepository.save(hero);
		return updatedHero;
}
````

<br/>

___

<br/>

### Post

##### hero.service.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
    /** POST: add a new hero to the server */
  addHero(hero: Hero): Observable<Hero> {
    return this.http.post<Hero>(this.heroesUrl, hero, this.httpOptions)
    .pipe(
      tap((newHero: Hero) => this.log(`added hero w/ id=${newHero.id}`)),
      catchError(this.handleError<Hero>('addHero'))
    );
  }
}
```

- Http Header 옵션 설정
- Post 함수 작성

#### heroes.component.ts

```typescript
add(name: string): void {
    name = name.trim();
if (!name) { return; }
this.heroService.addHero({ name } as Hero)
    .subscribe(hero => {
    this.heroes.push(hero);
});
```

- add 함수 추가

<br/>

#### ServerSide

##### HeroRestController.java

````java
@PostMapping
public Hero insertHero(@RequestBody Hero hero) {
    logger.info("Hero 등록 " + hero);
    return heroRepository.save(hero);
}
````

<br/>

___

<br/>

### Delete

##### hero.service.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
    /** DELETE: delete the hero from the server */
    deleteHero(id:number): Observable<Hero> {
      const url = `${this.heroesUrl}/${id}`;
      return this.http.delete<Hero>(url, this.httpOptions).pipe(
      tap(_ => this.log(`deleted hero id=${id}`)),
      catchError(this.handleError<Hero>('deleteHero'))
      );
   }
}
```

- Http Header 옵션 설정
- Delete 함수 작성
- 반환할 값이 없기 때문에 void로 작성

##### heroes.component.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
/** DELETE: delete the hero from the server */
    deleteHero(id:number): Observable<Hero> {
        const url = `${this.heroesUrl}/${id}`;
    return this.http.delete<Hero>(url, this.httpOptions)
    .pipe(
    tap(_ => this.log(`deleted hero id=${id}`)),
    catchError(this.handleError<Hero>('deleteHero'))
   	);
  }
}
```

- delete 함수 추가

<br/>

#### ServerSide

##### HeroRestController.java

````java
@DeleteMapping("/{id}")
public Hero deleteHero(@PathVariable Long id) {
    logger.info("Hero 삭제 :" + id);
    Hero hero = heroRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Hero","id", id));
    heroRepository.delete(hero);
    return hero;
}
````

<br/>

___

<br/>

### Search

##### hero.service.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
    /* GET heroes whose name contains search term */
    searchHeroes(term: string): Observable<Hero[]> {
        if (!term.trim()) {
            // if not search term, return empty hero array.
            return of([]);
    }
    return this.http.get<Hero[]>(`${this.heroesUrl}/?name=${term}`)
        .pipe(
            tap(x => x.length ?
            this.log(`found heroes matching "${term}"`) :
            this.log(`no heroes matching "${term}"`)),
            catchError(this.handleError<Hero[]>('searchHeroes', []))
    	);
    }
}
```

- Http Header 옵션 설정
- Delete 함수 작성
- 반환할 값이 없기 때문에 void로 작성

##### heroes.component.ts

```typescript
...
httpOptions = {headers: new HttpHeaders({ 'Content-Type': 'application/json' })};
...{
/** DELETE: delete the hero from the server */
    deleteHero(id:number): Observable<Hero> {
        const url = `${this.heroesUrl}/${id}`;
    return this.http.delete<Hero>(url, this.httpOptions)
    .pipe(
    tap(_ => this.log(`deleted hero id=${id}`)),
    catchError(this.handleError<Hero>('deleteHero'))
   	);
  }
}
```

- search함수 추가

<br/>

#### ServerSide

##### HeroRestController.java

````java
@GetMapping // name을 파라미터로 받지만, 필수 값은 아니고 기본 값을 ""로 설정
public List<Hero> searchHeroes (@RequestParam(required = false, defaultValue="")String name){
    if(name.equals("")) {
        logger.info("Hero 전체 목록 조회");
        return heroRepository.findAll();
    }else {
        logger.info("Hero 검색어 조회" + name);
        return heroRepository.finaByName(name);
    }
}
````

- 메서드 재활용

