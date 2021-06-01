## Angular 7.0 프로그래밍 (2021.06.01)

- 전날 설치했던 Augury - Chrome Extention 제거 후 [Angular Devtools](https://chrome.google.com/webstore/detail/angular-devtools/ienfalfjdbdpebioblfackkekamfmbnh) 로 대체

- [앵귤러 개발 가이드](https://angular.io/guide/styleguide)

<br/>

### 1. Angular Data Binding

![overview](https://angular.io/generated/images/guide/architecture/overview2.png)

![Data Binding](https://angular.io/generated/images/guide/architecture/databinding.png)

___

<br/>

#### * value binding

- One - way Binding을 지원
- `{{ }}` : 컴포넌트(ts)에 정의 된 변수나 메서드를 템플릿에서 출력할 때 사용

<br/>

#### * property binding

- One - way Binding을 지원
- `[property] = "value"` : 템플릿에서 HTML 엘리먼트의 속성 값이나 동적 변수를 출력 할 때 사용

```typescript
imgUrl = "aa.jpg"";
<img [src] = "imgUrl">
    
`or`

<div [ngClass]="{'special' : isSpecial}"></div>
```

<br/>

#### * event binding

- One - way Binding을 지원
- `(event) = "handler"` : 템플릿에서 event가 발생 했을 때를 위해 컴포넌트에 event handler를 작성해야 한다.
  - 데이터를 보내거나 함수를 호출할 때 사용

```html
// hero.component.html input tag
<div>
  <label>Hero name :</label>
  <input type = "text" [value]="hero.name" (keyup)="keyupHeroName($event)"/>
</div>

```

<br/>

```typescript
// hero.component.ts
  keyupHeroName(event : any) {
    this.hero.name = event.target.value;
  }
```

- Text box에 입력한 값을 Component 값으로 전달

<br/>

#### * Direcitve

- common module은 따로 import하지 않아도 사용 가능

<br/>

##### **[Structural Directive : ngFor, ngIf](https://angular.io/api/common/NgForOf)

```html
<li *ngFor = "let hero of heroes"></li>
<app-hero-detail *ngIf = "selectedHero"></app hero-detail>
```

- for 문, if 문과 유사하다

##### 사용 예시

```html
<li *ngFor="let user of users; index as i; first as isFirst">
  {{i}}/{{users.length}}. {{user}} <span *ngIf="isFirst">default</span>
</li>
```

<br/>

##### **Attribute Directive : ngModel

- Two - way Binding을 지원
- `[(ngModel)] = "property"` : property binding + event binding
- ngModel 사용을 위해선 `App.module.ts`에 추가 모듈 Import 필요
  - AppModule : Component 묶음

<br/>

##### App.module.ts

```typescript
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { HeroesComponent } from './heroes/heroes.component';

@NgModule({
  declarations: [
    AppComponent,
    HeroesComponent
  ],
  imports: [
    BrowserModule,
      // FormsModule import를 해야 사용 가능
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

<br/>

##### heroes.component.html

```html
<div
	<label for ="hero name"> Hero name : </labal>
	<input id = "hero name" [(ngModel)] = "hero.name" placeholder="name">
</div>
```

- event binding과 value binding이 동시에 진행 된다

<br/>

##### \<label for/>

```html
  <label for="hero-name">Hero name :</label>
  <input id = "hero-name" [(ngModel)] = "hero.name" placeholder="name">
```

- 레이블을 눌러도 input 칸에 커서가 향하게 됨

___

<br/>

##### == vs ===

- `==`는 같은지 비교
- `===`는 타입까지 같은지 비교

<br/>

___

<br/>

### 2. Displaying a List

##### mock-heroes.ts

```typescript
import { Hero } from './hero';

export const HEROES: Hero[] = [
  { id: 11, name: 'Mr. Nice' },
  { id: 12, name: ' Narco ' },
  { id: 13, name: ' Bombasto ' },
  { id: 14, name: ' Celeritas ' },
  { id: 15, name: 'Magneta ' },
  { id: 16, name: 'RubberMan ' },
  { id: 17, name: ' Dynama ' },
  { id: 18, name: 'Dr IQ' },
  { id: 19, name: 'Magma' },
  { id: 20, name: 'Tornado' },
];
```

<br/>

##### heroes.component.html

```html
<!--<h1>{{hero}}</h1>-->
<!--<h1>{{hero.name|uppercase}}</h1> 대문자로 출력-->
<h2>My Heroes</h2>
  <ul class="heroes">
  <li *ngFor ="let hero of heroes; index as i; first as isFirst"
    [class.selected]="hero === selectedHero"
    (click)="onSelect(hero)">
    <!-- {{i}}/{{heroes.length}} -->
     <span class="badge">{{hero.id}}</span> {{hero.name}}
     <!-- <span *ngIf = "isFirst">Default</span> -->
    </li>
  </ul>


<app-hero-detail [hero]="selectedHero"></app-hero-detail>

```

<br/>

##### heroes.component.ts

````typescript
import { Component, OnInit } from '@angular/core';
import{ Hero} from '../hero';
import { HEROES } from '../mock-heroes';

@Component({
  selector: 'app-heroes',
  templateUrl: './heroes.component.html',
  styleUrls: ['./heroes.component.css']
})
export class HeroesComponent implements OnInit {
  //hero = 'WindStorm';
  hero : Hero = {id:1, name:'WindStorm'};
  heroes:Hero[] = HEROES;
  selectedHero ? :Hero;


  constructor() {
    console.log("HeroesComponent 생성자 호출 됨");
}
  ngOnInit(): void {
    console.log("HeroesComponent ngOnInit 메서드 호출 됨");
  }

  keyupHeroName(event:any) :void {
    this.hero.name = event.target.value;
  }

  onSelect(hero:Hero):void {
    this.selectedHero = hero;
  }

}
````

<br/>

##### heros-detail.component.ts

````typescript
import { Component, Input, OnInit } from '@angular/core';
import{Hero} from '../hero';

@Component({
  selector: 'app-hero-detail',
  templateUrl: './hero-detail.component.html',
  styleUrls: ['./hero-detail.component.css']
})
export class HeroDetailComponent implements OnInit {
	// 데이터를 넘겨주는 Input 데코레이터
  @Input() hero?:Hero;

  constructor() {

   }

  ngOnInit(): void {
  }

}
````

<br/>

##### hero-detail.component.html

```html
<div *ngIf="hero">
  <h1>{{hero.name|titlecase}}</h1><!--첫글자만 대문자로 출력-->
  <div><span>id :</span>{{hero.id}}</div>
  <div>
    <label for="hero-name">Hero name :</label>
    <!-- <input type = "text" [value]="hero.name" (keyup)="keyupHeroName($event)"/> -->
    <input id = "hero-name" [(ngModel)] = "hero.name">
  </div>
</div>

```

<br/>

___

<br/>

### 3. Service

```shell
ng generate service [서비스명]
```

<br/>

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import {Hero} from './hero';
import {HEROES} from './mock-heroes';

@Injectable({
  providedIn: 'root'
})
export class HeroService {

  constructor() { }
  getHeroes() : Hero [] {
      return HEROES;
    }
}

```

- HeroService는 DI(Dependency Injection) 를 사용하여 HeroesComponent 에 주입 (inject) 되어진다

<br/>

##### heroes.component.ts

```typescript
import { Component, OnInit } from '@angular/core';
import{ Hero} from '../hero';
import { HEROES } from '../mock-heroes';
import {HeroService} from '../hero.service';


@Component({
  selector: 'app-heroes',
  templateUrl: './heroes.component.html',
  styleUrls: ['./heroes.component.css']
})
export class HeroesComponent implements OnInit {
  //hero = 'WindStorm';
  hero : Hero = {id:1, name:'WindStorm'};
  heroes:Hero[] = HEROES;
  selectedHero ? :Hero;

	//생성자에 히어로서비스 주입
  constructor(private heroService : HeroService) {
    console.log("HeroesComponent 생성자 호출 됨");
}
  ngOnInit(): void {
    console.log("HeroesComponent ngOnInit 메서드 호출 됨");
    this.getHeroes();
  }

  keyupHeroName(event:any) :void {
    this.hero.name = event.target.value;
  }

  onSelect(hero:Hero):void {
    this.selectedHero = hero;
  }

  getHeroes() : void {
    this.heroes = this.heroService.getHeroes();
  }

}
```

<br/>

#### * Observable Service

- Observable은 [RxJS Library](http://reactivex.io/rxjs) 의 중요한 클래스이다
- Angular의 HttpClient 의 메서드들은 RxJS Observable 객체를 리턴 한다 .
- RxJS의 `of( )` function 은 Server 로 부터 데이터를 가져오는 것 처럼 simulate 하는 함수이다.
  - `of()`대신 HttpClient 의 `get()` 메서드 사용하기

<br/>

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import {Hero} from './hero';
import {HEROES} from './mock-heroes';

@Injectable({
  providedIn: 'root'
})
export class HeroService {

  constructor() { }
    // of 함수 사용 및 Observable 객체 타입 맞춰주기
  getHeroes() : Observable<Hero []> {
      return of(HEROES);
    }
}
```

- 값을 동적으로 받아오는 것이 아니기 때문에 getHeroes()에서 에러가 발생한다.
  - `.subscribe()`를 사용해서 값을 가져오고 Callback 함수에서 처리 한다.

<br/>

##### heroes.compoent.ts

```typescript
 getHeroes() : void {
    // this.heroes = this.heroService.getHeroes();
     //히어로 서비스에서 of()메서드로 생성한 가상 값을 .subscribe 함수로 받아 옴
    this.heroService.getHeroes().subscribe(heroesObj => this.heroes = heroesObj);
  }
```

<br/>

#### * Message Service

```shell
ng generate component [컴포넌트명]
```

- App.component.html에 `<app-messages></app-messages>` 추가

<br/>

```shell
ng generate service [서비스명]
```

##### message.service.ts

```typescript
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  messages: string[] = [];

  constructor() { }

    add(message: string) : void{
      this.messages.push(message);
    }
    clear(): void {
      this.messages = [];
    }
}
```

<br/>

##### 1. HeroService에서 MessageService 호출

- `service-inservice` scenario

##### hero.service.ts

```typescript
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import {Hero} from './hero';
import { MessageService } from './message.service';
import {HEROES} from './mock-heroes';

@Injectable({
  providedIn: 'root'
})
export class HeroService {
	//생성자에 메세지 서비스 주입
  constructor(private messageService: MessageService) { }
    //메세지 서비스에서 add를 호출해 사용하는 getHeroes() 메서드 생성
  getHeroes() : Observable<Hero []> {
      this.messageService.add(`HeroService: fetched heroes`);
      return of(HEROES);
    }
}
```

<br/>

##### 2. MessageComponent에서 MessageService 호출

- 기존에는 Typescript 내부에서만 사용 했기 때문에 private으로 했지만 템플릿에서 사용을 원할 경우  public으로 설정

##### message.component.ts

```typescript
import { Component, OnInit } from '@angular/core';
import { MessageService } from '../message.service';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.css']
})
export class MessagesComponent implements OnInit {
	
    //생성자에  메세지 서비스 주입
  constructor(public messageService : MessageService) { }

  ngOnInit(): void {
  }

}
```

<br/>

##### message.component.html

```html
<!-- 메세지서비스의 메세지가 있을 경우 출력 -->
<div *ngIf="messageService.messages.length">
  <h2>Messages</h2>
  <button class="clear"    
  (click)="messageService.clear()">Clear messages</button>
  <!-- 메세지서비스의 메시지를 출력-->
    <div *ngFor= 'let message of messageService.messages'> {{message}} </div>
</div>
```

<br/>

##### 3. HeroesComponent에서 MessageService 호출

```typescript
import { HeroService } from '../hero.service';
export class HeroesComponent implements OnInit {
   	 selectedHero?: Hero;
  	  heroes: Hero[] = [];
    //생성자에 메시지 서비스 주입
  	  constructor(private heroService: HeroService, private messageService: MessageService) {
    }
    ngOnInit() {
  	  this.getHeroes();
    }
    onSelect(hero: Hero): void {
   	 this.selectedHero = hero;
   	 this.messageService.add(`HeroesComponent: Selected hero id=${hero.id}`);
    }
    getHeroes(): void {
   	 this.heroService.getHeroes().subscribe(heroesObj => this.heroes = heroesObj);
    }
}
```

