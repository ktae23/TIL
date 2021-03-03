## 자바스크립트 기본 용어

### 기본 용어

##### 표현식과 문장

- 표현식 : 프로그램 실행 최소 코드 단위
- 세미 콜론 또는 줄 바꿈으로 문장 종료
  - 표현식이 모여 문장, 문장이 모여 프로그램

<br />

```javascript
// 표현식
523;
50 + 29 / 12 * 59;
var name = '김' + '이' + '박';
arlert('알럿 출력');

// 문장
539
10 + 59 + 2 / 4 * 7
'문장'
```

<br />

##### 키워드

- 자바스크립트 제작 시 정해진 의미가 부여 된 단어 (like 예약어)

##### 지원되는 30가지 키워드

| 자바     | 스크립트 | 키워드     |         |         |          |
| -------- | -------- | ---------- | ------- | ------- | -------- |
| break    | else     | instanceof | true    | case    | false    |
| new      | try      | catch      | finally | null    | typeof   |
| continut | for      | return     | var     | default | function |
| switch   | void     | delete     | if      | this    | while    |
| do       | in       | throw      | with    | const   | class    |

<br />

##### 식별자

- 변수나 함수 등에 이름을 붙일 대 사용하는 단어
- 사용할 수 없는 규칙이 있음
  - 키워드 사용 불가
  - 특수 문자는 _와 $ 이외 사용 불가
  - 숫자로 시작 불가
  - 공백 입력 불가
- 관례
  - 알파벳 사용
  - 생성자 함수 이름은 항상 대문자로 시작
  - 변수, 인스턴스, 함수, 메서드의 이름은 항상 소문자로 시작
  - 여러 단어의 식별자는 각 단어 첫 글자 대문자
- 의미를 알 수 있는 단어 사용 권장

<br />

| 구분                  | 단독 사용 | 다른 식별자와 함께 사용 |
| --------------------- | --------- | ----------------------- |
| 식별자 뒤에 괄호 없음 | 변수      | 속성                    |
| 식별자 뒤에 괄호 있음 | 함수      | 메서드                  |

```javascript
alert('알럿 출력')	// 함수
Array.length	// 속성
input	// 변수
```

<br />

##### 주석

```javascript
// 한 행 주석 처리
/*
	여러 행 주석 처리
*/
```

<br />

### 자바스크립트 출력

- alert()함수가 가장 기본적인 출력 방법
- 크롬 웹브라우져에서 `f12`를 눌러 `console`창에서 코드 입력 후 실행하여 빠른 확인 가능

<br />

## 자료형과 변수

### 자료형

#### 숫자

- 정수와 실수를 구분하지 않음
  - +(덧셈), -(뺄셈), *(곱셈), /(나눗셈), %(나머지)

<br />

#### 문자열

- 문자 집합
- 큰 따옴표, 작은 따옴표 상관 없음
  - 작은 따옴표 안에 큰 따옴표를 넣어 사용하는 경우가 많음
- 이스케이프 문자

| 이스케이프 문자 | 설명        |
| --------------- | ----------- |
| \t              | 수평 탭     |
| \n              | 행 바꿈     |
| \\              | 역 슬래시   |
| \'              | 작은 따옴표 |
| \"              | 큰 따옴표   |

- 문자열 연결 연산

| 연산자 | 설명        |
| ------ | ----------- |
| +      | 문자열 연결 |

<br />

#### bool

- 참과 거짓을 표현할 때 사용
- 비교 연산자

| 연산자 | 설명                        |
| ------ | --------------------------- |
| >=     | 좌변이 우변보다 크거나 같음 |
| <=     | 우변이 좌변보다 크거나 같음 |
| >      | 좌변이 우변보다 큼          |
| <      | 우변이 좌변보다 큼          |
| ==     | 양변이 같음                 |
| !=     | 양변이 다름                 |

<br />

- 논리 연산자

| 연산자 | 설명                                 |
| ------ | ------------------------------------ |
| !      | 논리 부정 ( 참과 거짓 반대 )         |
| &&     | 논리 곱 ( 둘다 참이어야 참 )         |
| \|\|   | 논리 합 ( 둘 중 하나만 참이어도 참 ) |

<br />

#### Undefined

- 변수의 자료형이 정해지지 않았음을 나타 냄
  - 변수는 존재하나 초기화(값 할당)가 되지 않은 상태
- Null은 변수는 존재하나 null로 값이 할당 된 상태로 자료형이 정해진 상태
  - Undefined와는 다른 값
- 자바스크립트의 `==` 연산자의 자동 형 변환으로 인해 undefined와 null은 true 값이 나옴
  - 타입까지 엄격하게 검사하는 `===` 연산자를 사용하면 false 나옴

[Undefined 참조](https://siyoon210.tistory.com/148)

<br />

### 변수

- 값을 저장할 때 사용하는 식별자
- 모든 자료형 저장 가능
  - 1. 변수 선언
    2. 변수 초기화 ( 변수에 값을 할당)

<br />

## 조건문과 반복문

### 조건문

##### 중첩 조건문

- 여러번 중첩해도 상관 없음

```javascript
﻿<script>
    // Date 객체를 선언합니다: 현재 시간 측정
    var date = new Date();
    var hours = date.getHours();

    // 조건문
    if (hours < 5) {
        alert('잠을 자렴....');
    } else {
        if (hours < 7) {
            alert('준비');
        } else {
            if (hours < 9) {
                alert('출근');
            } else {
                if (hours < 12) {
                    alert('빈둥빈둥');
                } else {
                    if (hours < 1) {
                        alert('식사');
                    } else {
                        // 여러 가지 업무를 수행합니다.
                    }
                }
            }
        }
    }
</script>
```

<br />

##### if - else if문

- 중복되지 않는 세 가지 이상의 조건을 구분 할 때 사용

```javascript
﻿<script>
    // Date 객체를 선언합니다: 현재 시간 측정
    var date = new Date();
    var hours = date.getHours();

    // 조건문
    if (hours < 5) {
        alert('잠을 자렴....');
    } else if (hours < 7) {
        alert('준비');
    } else if (hours < 9) {
        alert('출근');
    } else if (hours < 12) {
        alert('빈둥빈둥');
    } else if (hours < 14) {
        alert('식사');
    } else {
        // 여러 가지 업무를 수행합니다.
    }
</script>
```

<br />

### 반복문

##### 배열

- 객체의 일종으로 대괄호 [] 를 사용해 생성
  - 입력된 자료 하나하나를 `요소`라고 부름
  - 다양한 자료형 입력 가능
  - 전체 출력 시 순서대로 요소가 표시 됨
  - 인덱스를 이용한 접근, 요소 변경 가능
  - .length 속성을 이용해 길이 출력 가능

<br />

##### while 반복문

- 자바의 while문과 같은 형태의 반복문 사용

<br />

##### for 반복문

- 자바의 for문과 같은 형태의 반복문 사용

```javascript
<script>
    for (var i = 0; i < 100; i++) {
        alert('출력');
    }
</script>
```

<br />

## 함수

- 코드의 집합

### 선언과 호출, 실행 우선 순위

##### 선언과 호출

- 익명 함수
  - function () {}
- 선언적 함수
  - function 함수 () {}

<br />

```javascript
// 선언적 함수
<script>
    // 함수를 선언합니다.
    function 함수() {
        alert('함수_01');
        alert('함수_02');
    };
</script>

// 익명 함수
<script>
    // 함수를 선언합니다.
    var 함수 = function () {
        alert('함수_01');
        alert('함수_02');
    };
</script>

```

<br />

##### 실행 우선순위

- 함수도 변수이므로 여러 차레 할당 했을 경우 가장 마지막에 입력 된 값이 저장 됨
- 실행 시 선언적 함수를 모든 코드보다 먼저 읽고 익명 함수를 나중에 읽기 때문에 같은 함수로 선언 할 경우 익명 함수가 저장 됨

<br />

### 매개변수와 반환 값

- 매개변수 : 함수의 괄호 안에 입력하여 전달되는 값
- 반환 값 : 함수를 실행한 결과 값

<br />

### 콜백 함수

- 매개변수로 전달되는 함수

```javascript
<script>
    // 함수 선언
    function callTenTimes(callback) {
        // 10회 반복
        for (var i = 0; i < 10; i++) {
            // 매개 변수로 전달된 함수 호출
            callback();
        }
    }
    // 변수 선언
    var callback = function () {
        alert('함수 호출');
    };

    // 함수 호출
    callTenTimes(callback);
</script>

// 익명 함수로 사용 하는 경우가 많음

<script>
    // 함수 선언
    function callTenTimes(callback) {
        for (var i = 0; i < 10; i++) {
            callback();
        }
    }

    // 함수 호출
    callTenTimes(function () {
        alert('함수 호출');
    });
</script>
```

<br />

## 객체

- 자료형 여러개를 key - value 기반으로 한 번에 저장
  - 배열은 객체를 기반으로 만들어짐
  - 자바에서의 map, 파이썬에서의 Dictionary
- 객체변수[키] == 키에 해당하는 값

<br />

##### for in 반목문을 활용하여 객체 요소 하나씩 살펴보기

```javascript
﻿<script>
    // 객체를 선언합니다.
    var product = {
        제품명: '7D 건조 망고',
        유형: '당절임',
        성분: '망고, 설탕, 메타중아황산나트륨, 치자황색소',
        원산지: '필리핀'
    };

    // 출력합니다.
    for (var i in product) {
        alert(i + ':' + product[i]);
    }
</script>
```

<br />

### 속성과 메서드

- 배열에 있는 값 하나하나를 요소(Element)라고 함
- 객체에 있는 값 하나하나를 속성(Property)라고 함
  - 요소와 속성이 자바스크립트에서는 같은 의미로 사용 됨
- 함수도 속성으로 입력 가능
  - 요소로 입력 된 함수를 메서드라고 함
- 객체에 있는 속성을 분명히 표시할 때는 `this.속성`으로 표현

<br />
