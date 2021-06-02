## Angular 7.0 프로그래밍 (2021.06.02)

### Observer Pattern

- Gof Pattern (디자인 패턴) 중 하나

![](https://img1.daumcdn.net/thumb/R1280x0/?scode=mtistory2&fname=http%3A%2F%2Fcfile29.uf.tistory.com%2Fimage%2F245ECB3652EC11861953ED)

<br/>

- 행위 패턴
  - 일방적 통지 방식
- one to many depoendency
  - 한 객체의 상태가 바뀌면 그 객체에 의존하는 다른 모든 객체에게 통지를 하여 모두의 내용이 갱신 되는 패턴
  - notify()를 하면서 update()를 호출

<br/>

### [RXJS](https://www.learnrxjs.io/learn-rxjs/operators)

- 서버와 비동기 통신을 하기 위한 라이브러리

```typescript
import { Component, OnInit } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { Hero } from '../hero';
import { HeroService } from '../hero.service';
@Component({
  selector: 'app-hero-search',
  templateUrl: './hero-search.component.html',
  styleUrls: [ './hero-search.component.css' ]
})

export class HeroSearchComponent implements OnInit {
    //?(Optional)를 붙여 값이 없을수도 있음을 명시했지만 꼭 사용하는 값이라면 생성자에서 명시를 해도 된다.
    heroes$?: Observable<Hero[]>;
    private searchTerms = new Subject<string>();
    constructor(private heroService: HeroService) {}
    // Push a search term into the observable stream.
    search(term: string): void {
        this.searchTerms.next(term);
    }

    ngOnInit(): void {
      this.heroes$ = this.searchTerms
        .pipe(
            // 300 밀리 초 동안 기다린 후 최신 문자열 전달
            debounceTime(300),
            // 필터 텍스트가 변경된 경우에만 요청 전송
            distinctUntilChanged(),
            // debounce 및 distincUntilChanged를 통해 검색하는 각 검색어에 대해 검색 서비스를 호출
            switchMap((term: string) => this.heroService.searchHeroes(term)),
        );
    }

  }
```

- Subject는 observable values의 source이며 observable 자체이다
  - Observable 객체 모음..?

<br/>

### Form

- Angular의 두가지 Form-building 방식 
- 두 방식 모두 @angular/forms 라이브러리에 속한다. 

<br/>

- Template-driven forms
  - FormsModule 사용 
  - Template-driven forms 방식은 form control object를 사용자가 만들지 않고, Angular가 form control object를 생성
    -  ngModel를 제공 하여 Angular가 핸들링
    -  이벤트가 발생하면 Angular는 mutable 데이터 모델 업데이트

<br/>

- Reactive forms
  - ReactiveFormsModule 사용 
  - Reactive forms는 컴포넌트 클래스에 form control object생성
    - 이를 컴포넌트 템플릿의 form control 엘리먼트에 바인딩
  -  컴포넌트 클래스는 데이터 모델과 form control 구조에 즉시 액세스 할 수 있으므로 데이터 모델 값을 form control로 보내고, 사용자가 변경 한 값을 다시 가져올 수 있음 
  -  Reactive forms의 장점
    -  값 및 유효성 업데이트가 항상 동기적
    - 사용자가 제어 할 수 있음

<br/>

