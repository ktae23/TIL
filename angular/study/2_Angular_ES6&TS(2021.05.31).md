## Angular 7.0 프로그래밍 (2021.05.31)

### 1. ECMAScript6

- [JSbin](https://jsbin.com/?html,output)에서 온라인 상에서 코드 실행해 볼 수 있음
  - JS와 Console 확인

###### var / let.const

```javascript
function foo() {
    var / let a = 'hello';
    
    if (true) {
       var / let a = 'bye';
        console.log(a); // bye
    }
    console.log(a); // bye
}
foo();
const value = 10;
// value = 11; 
const myArr = [1,2];
myArr.push(10);
console.log(myArr);
// [1,2,10]
const myObj1 = {a:10};
const myObj2 = {b:20};
const myObj3 = Object.assign({},myObj1,myObj2);
console.log(myObj3);
/* 
[object Object] {
	a:10,
    b:20
}
*/
//const : block scope => 값 타입일 경우 상수이기 때문에 한번 선언하면 변경 불가지만, Array, Object 등 참조 타입인 경우 추가, 삭제 가능
//let : block scope => 'bye', 'hello'
//var : function scope => 'bye', 'bye'
```

<br/>

##### 삼항 조건 연산자

```javascript
const
x = 20;
let answer;
if (x > 10) {
	answer =
	greater than 10
} else {
	answer =
	less than 10
}

==(위와 아래는 같은 기능을 하는 코드)

const answer = x > 10 ? ' greater than 10 '': less than 10 '';

//react에서 특정 버튼을 state 값에 따라 보여지게 할 경우에 이렇게 사용할 수 있음
{editable ? (
	<a onClick ={() => this.save record.key )}> </>
) : (
<a onClick ={() => this.edit record.key )}> </a>
)}
```

<br/>

##### For 루프

```javascript
const myArr = [10.20.30];
//기존
for (let i =0; i < msgs.length ; i ++){
    console.log(i + " " + myArr[i]);
}
    
//for 축약 기법
//for-in
for(let val in myArr){
  console.log(val + ' ' + myArr[val]);
}
  
//for-of
for (letvalue of msgs ){
    console.log(val);
}

//Array.forEach 축약 기법
function logArrayElements(element,index , array)
	console.log('a[' + index + '] = '+ element);
}
[2,5,9].forEach logArrayElements
//a[0] = 2
//a[1]= 5
//a[2] = 9
```

<br/>

##### Arrow function 화살표 함수, 람다식

```javascript
//forEach
function sayHello(msg){
    return 'Hello' + msg;
}
console.log(sayHello('Javascript'));

`or`

let sayHello2 = msg => 'Hello' +msg;
console.log(sayHello2('람다식'));

`or`

sayHello2 = msg => {
    return 'Hello' + msg;
}

`or`

sayHello2 = msg => ('Hello' + msg);
// 구현부가 한 줄일 경우 return 생략 가능
console.log(sayHello2('람다식'));
```

<br/>

##### map(), filter(), reduce()

- 인자 값으로 함수가 들어 감 (람다식으로 입력)

```javascript
const myArr = [10,20,30,40,60];
//map() - 매핑
let result = myArr.map(item => item + 10);
console.log(result);

let result2 = myArr.map((item,idx) => item + idx);
console.log(result2);

//filter() - 조건식
//3의 배수인 값만 반환하라
const result3 = myArr.filter(item => item % 3 == 0);
console.log(result3);

//reduce() - 합계
const sum = myArr.reduce((prev,curr)=> prev + curr);
//prev : 직전 값, curr : 현재 값
console.log(sum);
```

<br/>

##### Arrow function과 기존 function과의 차이 : (this)의 사용

```javascript
//생성자 함수
function BlackDog (){
	this.name = '흰둥이';
	return {
		name:'검둥이' //일반 function에서의 this
		bark: function() {
			console.log(this.name + '멍멍');
        }
	}	
}
const blackDog = new BlackDog();
blackDog.bark(); // 검둥이 멍멍

===============================================================
//생성자 함수
function WhiteDog (){
    this.name = '흰둥이'; //Arrow function에서의 this
	return {
		name:'검둥이'
		bark: () => {
			console.log(this.name + '멍멍');
		}
	}
}
const whiteDog = new WhiteDog();
whiteDog.bark(); // 흰둥이 멍멍
```

<br/>

##### Default Prameter Values(파라미터 기본 값 지정하기)

- 기존에는 if 문을 통해서 함수의 파라미터 값에 기본 값을 지정해 줘야 했다
- ES6 에서는 함수 선언문 자체에서 기본값을 지정해 줄 수 있음

```javascript
volume = (l, w = 3, h = 4 ) => (l * w * h);
volume(2) 
//output:24
```

<br/>

##### 템플릿 리터럴 (Template Literals)

```javascript
백틱 (backtick) 을 사용해서 스트링을 감싸고 , 를 사용해서 변수를 담아 준다

const host = 'aa.com';
const port = 8090;

//기존
const welcome = 'You have logged in as ' + first + ' ' + last + '.'
const db = 'http://' + host + ':' + port + '/' + database;

//축약
const welcome = `You have logged in as ${first} ${last}`;
const db = `http://${host}:${port}/${database}`;
```

<br/>

##### 비구조화 할당 (Destructuring Assignment)

- 데이터 객체가 컴포넌트에 들어가게 되면 , unpack 이 필요합니다

```javascript
//Array destructuring assignment (비구조화 할당)
let a, b, rest;
[a, b] = [1, 2];
console.log(a);
console.log(b);

let foo = ["one", "two", "three"];

let [foo1, foo2, foo3] = foo;
console.log(foo1);
console.log(foo2);
console.log(foo3);

//swapping
let a = 1;
let b = 3;
[a, b] = [b, a];
console.log(a);
console.log(b);

//Object destructuring assignment (비구조화 할당)
let obj = {p: 42, q: true}; 
let val1 = obj.p;
console.log(val1);
let val2 = obj.q;
console.log(val2);

let {p, q} = obj;
console.log(p);
console.log(q);

//react 에서의 비구조화 할당
const observable = require('mobx/observable');
const action = require('movx/action');
const runInAction = require('mobx/runInAction');

const store = this.props.store
const form = this.props.form
const loading = this.props.loading
const errors = this.props.errors
const entity = this.props.entity
                     
//축약
import { observable, action, runInAction } from mobx
const { store, form, loading, errors, entity } = this.props ;
```

<br/>

##### 전개연산자 (Spread Operateor)

- concat 함수와는 다르게 전개 연산자를 이용하면 하나의 배열을 다른 배열의 아무 곳에나 추가할 수 있습니다
- 전개 연산자는 ES6 의 구조화 대입법 (destructuring notation) 와 함께 사용할 수도 있습니다

```javascript
//기존
const odd = [1, 3, 5 ];
const nums = [2, ...odd, 4 , 6 ];[2,1,3,5,4,6]

//cloning arrays
const arr = [1,2,3,4];
const arr2 = arr.slice();
console.log(arr2);//[1,2,3,4]

const arr3 = [...arr];
console.log(arr3);//[1,2,3,4]


//축약
const { a, b, ...rest } = { a: , b: 2, c: 3, d: 4);
console.log(a) // 1
console.log(b) // 2
console.log(rest) // { c: 3, d: 4}
const {c,d} = rest;
console.log(c); 3
console.log(d); // 4


let x = 100;
let y = 200;
const obj = {x,y} //{x:x, y:y}
// 왼쪽의 키 값고 오른쪽의 변수의 이름이 동일 할 경우 한번만 명시
console.log(obj);
```

<br/>

##### import/export

```javascript
//export
export const myNumbers = [1, 2, 3,
const animals = ['Panda', 'Bear', 'Eagle'];

//export defulat는 1회만 가능
export default function myLogger (){
	console.log(myNumbers , pets);
}

export class Alligator {
	constructor() {
	// ...
	}
}
==============================
//import
//Importing with alias // as [이름]으로 불러서 사용 가능
import myLogger as Logger from 'app.js';

//Importing all exported members
import * as Utils from 'app.js';

Utils.myLogger();

//Importing a module with a default member
// defualt는 바로 이름으로 적어 줌
import myLogger from 'app.js';
// default가 아닌 경우 비구조화 할당
import myLogger , { Alligator, myNumbers } from 'app.
```

<br/>

### 2. [TypeScript](https://www.typescriptlang.org/)

- 자바스크립트의 슈퍼셋인 오픈소스 프로그래밍 언어로  C#, 델파이, 터보 파스칼의 창시자가 개발에 참여하여 클라이언트 사이드와 서버 사이드를 위한 개발에 사용 가능
- 객체 지향 언어로 JAVA와도 유사함
- 타입이 있기 때문에 반환 값을 신뢰 할 수 있음

##### 타입스크립트 설치 및 실행

```typescript
npm i -g typescript
// i는 install의 약어
// -g를 넣으면 node_modules에 설치 됨
// --save를 넣으면 pacakage.json에 기록 됨

// npx tsc
// 설치하지 않고 실행만 할때

// ts 컴파일
tsc [파일이름].ts
// TSC 는 타입스크립트를 자바스크립트로 변환해주는 도구

// js 실행
node [변환된 파일 이름].js
```

<br/>

#### Basic-type

```typescript
let isDone: boolean = false;

let decimal: number = 6; //숫자
let hex: number = 0xf00d; //16진수
let binary: number = 0b1010; //2진수
let octal: number = 0o744; //8진수

let color: string = "blue";
color = 'red';

let fullName: string = `Bob Bobbington`;
let age: number = 37;
let sentence: string = `Hello, my name is ${ fullName }.
I'll be ${ age + 1 } years old next month.`; //템플릿 리터럴
console.log(sentence);

let list: number[] = [1, 2, 3];
let list2: Array<number> = [1, 2, 3]; // 제네릭

// Declare a tuple type
let x: [string, number]; //튜플 //여러 타입을 동시에 줄 수 있음
// Initialize it
x = ["hello", 10]; // OK
// Initialize it incorrectly
//x = [10, "hello"]; // Error
console.log(x[0].substr(1)); // OK
//console.log(x[1].substr(1)); // Error, 'number' does not have 'substr'

enum Color {Red, Green, Blue} // 이넘(상수)
let c: Color = Color.Green;
console.log(c);

let colorName: string = Color[2]; // 문자열
console.log(colorName);

let notSure: any = 4; // 어떤 타입이든 가능
notSure = "maybe a string instead";
notSure = false; // okay, definitely a boolean
//notSure.ifItExists(); // okay, ifItExists might exist at runtime
//notSure.toFixed(); // okay, toFixed exists (but the compiler doesn't check)

//`<Type>Variable` or `Variable as Type` 
let someValue: any = "this is a string"; 
let strLength: number = (<string>someValue).length; 
//<String> any 타입을 String 타입으로 캐스팅
console.log(strLength);
==
let strLength2: number = (someValue as string).length; 
//<String> any 타입을 String 타입으로 캐스팅
console.log(strLength2);
```

<br/>

#### Class - Student

```typescript
class Student {
    //인스턴스 변수 - 내부에서만 사용할 경우 보통 변수명 앞에 _를 붙임
    private _fullName: string;
    //생성자
    constructor(public firstName, public middleInitial, public lastName) {
        this._fullName = firstName + " " + middleInitial + " " + lastName;
    }
	//getter method
    get fullName(): string {
        return this._fullName;
    }
	
    //setter method
    set fullName(newName: string) {
        this._fullName = newName;
    }
}

var user = new Student("Jane", "M.", "User");
console.log(user.firstName + ':' + user.middleInitial + ':' + user.lastName);
// 아래 user.fullName 은 getter Method를 호출 -> 괄호 생략
console.log(user.fullName);

// 아래 user.fullName 은 setter Method를 호출 -> 괄호 생략
user.fullName = 'john resig';
console.log(user.fullName);
```

- 그냥 `tsc [파일이름].ts` 할 경우 에러 남
  - tsc --target es5 [파일이름].ts
  - getter, setter와 같은 ES6만의 문법을 사용 할 때는es5로 낮춰주는 --target ES5 옵션을 줘야 함

<br/>

#### Inheritance

```typescript
class Animal {
    name: string;
    constructor(theName: string) { this.name = theName; }
    move(distanceInMeters: number = 0) {
        console.log(`${this.name} moved ${distanceInMeters}m.`);
    }
}

class Snake extends Animal {
    constructor(name: string) { super(name); }
    move(distanceInMeters = 5) {
        console.log("Slithering...");
        super.move(distanceInMeters);
    }
}

class Horse extends Animal {
    constructor(name: string) { super(name); }
    move(distanceInMeters = 45) {
        console.log("Galloping...");
        super.move(distanceInMeters);
    }
}

class Employee {
    private name: string;
    constructor(theName: string) { this.name = theName; }
}

// 부모 Animal 타입의 변수에 자식 Horse 객체를 생성해서 대입 가능
// 다형성 / 상속
let sam = new Snake("Sammy the Python");
let tom: Animal = new Horse("Tommy the Palomino");

sam.move();
tom.move(34);

console.log(new Animal("Cat").name);

let animal2 = new Animal("Goat");
let employee = new Employee("Bob");

animal2 = sam;
//animal2 = employee; // Error: 'Animal' and 'Employee' are not compatible
```

<br/>

#### Interface

```typescript
interface  Ivehicle {
    // {} body가 없는 추상 메서드만 선언 가능
    run();
}
class Car implements Ivehicle {
    name:string;
    constructor(public color:string, name:string){
        this.name = name;
    }
    // Method overriding
    run() : string {
        return `${this.color} 색 ${this.name} 이름의 자동차가가 달립니다.`;
    }
    run2() : void {
        console.log(`${this.color} 색 ${this.name} 이름의 자동차가가 달립니다.`);
    }
}


var car = new Car('red','마티즈');

console.log(car.run());
car.run2();

let car2 = new Car('Blue','소나타');
car2.run2();
```

