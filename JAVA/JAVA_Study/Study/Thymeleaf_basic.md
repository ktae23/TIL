### Thymeleaf

[Tutorial docs](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html)

#### dependency

```java
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

<br/>

#### HTML 파일 경로

`resources/template/*.html`

<br/>

#### 예시 페이지

```html
<!DOCTYPE HTML>
<html xmlns:th="http://www.thymeleaf.org">
<head> 
    <title>Getting Started: Serving Web Content</title> 
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
    <p th:text="'Hello, ' + ${name} + '!'" >샘플 데이터</p>
</body>
</html>
```

서버가 연결 되지 않았을 때도 HTML 구성을 따르기 때문에

 ```html
<p>샘플 데이터</p>
 ```

이렇게 나타나기 때문에 퍼블리셔, 디자이너가 작업하기에 좋음.

<br/>

개발자가 서버가 연결 될 때 보여질 코드를 작성하게 되는데, 서버가 연결 되면 th 태그를 읽어서 컨트롤러에서 데이터를 받아와 화면에 보여 준다.

```html
<p th:text="'Hello, ' + ${name} + '!'" ></p>
```

<br/>

#### 기본 태그

```html
변수 : ${...}
객체 변수 : *{...}
메시지 : #{...}
링크 : @{...}
레이아웃 조각 : ~{...}
```

<br/>

### 데이터 받기 예제들

#### 객체로 받기

```html
<div th:object="${session.user}">
    <p>Name: <span th:text="*{firstName}">Sebastian</span>.</p>
    <p>Surname: <span th:text="*{lastName}">Pepper</span>.</p>
    <p>Nationality: <span th:text="*{nationality}">Saturn</span>.</p>
</div>
```

#### 텍스트로 받기

```html
<div>
  <p>Name: <span th:text="${session.user.firstName}">Sebastian</span>.</p>
  <p>Surname: <span th:text="${session.user.lastName}">Pepper</span>.</p>
  <p>Nationality: <span th:text="${session.user.nationality}">Saturn</span>.</p>
</div>
```

#### 객체, 텍스트 혼합해서 받기

```html
<div th:object="${session.user}">
  <p>Name: <span th:text="*{firstName}">Sebastian</span>.</p>
  <p>Surname: <span th:text="${session.user.lastName}">Pepper</span>.</p>
  <p>Nationality: <span th:text="*{nationality}">Saturn</span>.</p>
</div>
```

- 통일 된 방법을 사용하는 것을 권장

<br/>

#### 문자 더하기

```html
<span th:text="'The name of the user is ' + ${user.name}">
```

==

```html
<span th:text="|Welcome to our application, ${user.name}!|">
```

==

```html
<span th:text="'Welcome to our application, ' + ${user.name} + '!'">
```

==

```html
<span th:text="${onevar} + ' ' + |${twovar}, ${threevar}|">
```

<br/>

#### 조건 표현

```html
<tr th:class="${row.even}? 'even' : 'odd'">
  ...
</tr>
```

==

```html
<tr th:class="condition ? then : else">
  ...
</tr>
```

조건식의 세 부분인 condition, then, else는 모두 그 자체로 표현식이기 때문에

변수(%{...}, *{...}), 메시지(#{...}), 링크@{...}, 리터럴('...')이 될 수 있음

<br/>

##### 중첩 조건 가능

```html
<tr th:class="${row.even}? (${row.first}? 'first' : 'even') : 'odd'">
  ...
</tr>
```

<br/>

##### else 생략, 값이 false면 null 반환

```html
<tr th:class="${row.even}? 'alt'">
  ...
</tr>
```

<br/>

#### Iteration

##### th:each

```html
<!DOCTYPE html>

<html xmlns:th="http://www.thymeleaf.org">

  <head>
    <title>Good Thymes Virtual Grocery</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <link rel="stylesheet" type="text/css" media="all" 
          href="../../../css/gtvg.css" th:href="@{/css/gtvg.css}" />
  </head>

  <body>

    <h1>Product list</h1>
  
    <table>
      <tr>
        <th>NAME</th>
        <th>PRICE</th>
        <th>IN STOCK</th>
      </tr>
      <tr th:each="prod : ${prods}">
        <td th:text="${prod.name}">Onions</td>
        <td th:text="${prod.price}">2.41</td>
        <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
      </tr>
    </table>
  
    <p>
      <a href="../home.html" th:href="@{/}">Return to home</a>
    </p>

  </body>

</html>
```

- `java.util.Iterable` 에 속하는 어떤 객체도 사용 가능
-  `java.util.Enumeration`에 속하는 어떤 객체도 사용 가능
- `java.util.Iterator`에 속하는 어떤 객체도 사용 가능, 모든 값을 메모리에 캐시 할 필요없이 iterator에 의해 반환 할 때 사용
- `java.util.Map`에 속하는 어떤 객체도 사용 가능 Map을 반복할 때 반복 변수들은  `java.util.Map.Entry`의 클래스가 된다
- 어떤 배열도 사용 가능.
- 이외 다른 객체들은 스스로를 단일 값으로 가진 List처럼 처리 됨

<br/>

```html
<ul th:if="${condition}">
  <li th:each="u : ${users}" th:text="${u.name}">user name</li>
</ul>
```

- 조건 설정을 통한 지연 검색을 사용하여 데이터의 실제 사용시에만 호출하도록 최적화를 할 수 있음

<br/>

#### Conditional Evaluation

##### th:if

```html
<!-- comments가 있는 경우에만 해당 링크를 생성-->
<td>
    <span th:text="${#lists.size(prod.comments)}">2</span> comment/s
    <a href="comments.html" 
       th:href="@{/product/comments(prodId=${prod.id})}" 
       th:if="${not #lists.isEmpty(prod.comments)}">view</a>
</td>
```

<br/>

##### th:unless

```html
<!-- comments가 있는 경우에만 해당 링크를 생성-->
<a href="comments.html"
   th:href="@{/comments(prodId=${prod.id})}" 
   th:unless="${#lists.isEmpty(prod.comments)}">view</a>
```

- if 의 역 속성

<br/>

##### th:switch

```html
<div th:switch="${user.role}">
  <p th:case="'admin'">User is an administrator</p>
  <p th:case="#{roles.manager}">User is a manager</p>
  <p th:case="*">User is some other thing</p>
</div>
```

- `th:case="*"`로 기본 값 설정
- 가장 먼저 하나의 th:case의 속성이 true가 되면 즉시 다른 case들은 false가 됨

<br/>

#### 우선 순위

| Order | Feature                         | Attributes                                 |
| :---- | :------------------------------ | :----------------------------------------- |
| 1     | Fragment inclusion              | `th:insert` `th:replace`                   |
| 2     | Fragment iteration              | `th:each`                                  |
| 3     | Conditional evaluation          | `th:if` `th:unless` `th:switch` `th:case`  |
| 4     | Local variable definition       | `th:object` `th:with`                      |
| 5     | General attribute modification  | `th:attr` `th:attrprepend` `th:attrappend` |
| 6     | Specific attribute modification | `th:value` `th:href` `th:src` `...`        |
| 7     | Text (tag body modification)    | `th:text` `th:utext`                       |
| 8     | Fragment specification          | `th:fragment`                              |
| 9     | Fragment removal                | `th:remove`                                |