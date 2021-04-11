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

#### Layout

##### copyright 표시 예제

- 별도 html 문서 작성 시

```html
<!DOCTYPE html>

<html xmlns:th="http://www.thymeleaf.org">

  <body>
  
    <div th:fragment="copy">
      &copy; 2011 The Good Thymes Virtual Grocery
    </div>
  
  </body>
  
</html>
```

- footer를 fragment로 지정 시

```html
<footer th:fragment="copy">
  &copy; 2011 The Good Thymes Virtual Grocery
</footer>
```

- 일반 CSS selector 이용 시 `~{footer :: #copy-section}`으로 선택 가능

```html
...
<div id="copy-section">
  &copy; 2011 The Good Thymes Virtual Grocery
</div>
...
```

<br/>

#### th:insert, th:replace, th:include 차이

##### th:insert

```html
<body>
    <div th:insert="footer :: copy"></div>
</body>
```

- 적용 결과

```html
<body>
    <div>
        <footer>
        	&copy; 2011 The Good Thymes Virtual Grocery
        </footer>
	</div>
</body>
<!--div 태그 안에 footer가 입력 됨 -->
```

<br/>

##### th:replace

- 많이 사용 됨

```html
<body>
	<div th:replace="footer :: copy"></div>
</body>
```

- 적용 결과

```html
<footer>
    &copy; 2011 The Good Thymes Virtual Grocery
</footer>
<!--div 태그가 footer로 대체 됨 -->
```

<br/>

##### th:include

- **thymeleaf 3.0 이후 include는 권장되지 않음**

```html
<body>
  <div th:include="footer :: copy"></div>
</body>
```

- 적용 결과

```html
<div>
    &copy; 2011 The Good Thymes Virtual Grocery
</div>
<!--div 태그 안에 footer의 내용만 포함 됨 -->
```

<br/>

#### 매개변수 입력을 이용한 fragment 사용

- 매개변수를 입력 받는 fragment 작성

```html
<div th:fragment="frag (onevar,twovar)">
    <p th:text="${onevar} + ' - ' + ${twovar}">...</p>
</div>
```

<br/>

- 사용

```html
<div th:replace="::frag (${value1},${value2})">...</div>
<div th:replace="::frag (onevar=${value1},twovar=${value2})">...</div>
```

- 매개변수 입력 순서는 중요하지 않음

<br/>

##### fragment 작성 시 매개변수를 입력하지 않았을 경우

```html
<div th:fragment="frag">
    ...
</div>
```

- 아래 구문처럼 매개변수를 초기화하여 입력할 경우 사용 가능

```html
<div th:replace="::frag (onevar=${value1},twovar=${value2})">...</div>
```

<br/>

#### 레이아웃의 사용

##### 1. header fragment 작성

```html
<!-- common_header fragment 호출 시 title과 추가 links 값을 매개변수로 입력 받도록 작성-->
<head th:fragment="common_header(title,links)">

  <title th:replace="${title}">The awesome application</title>

  <!-- 공통의 스타일시트와 스크립트 -->
  <link rel="stylesheet" type="text/css" media="all" th:href="@{/css/awesomeapp.css}">
  <link rel="shortcut icon" th:href="@{/images/favicon.ico}">
  <script type="text/javascript" th:src="@{/sh/scripts/codebase.js}"></script>

  <!--/* 각 페이지의 추가 링크를 위한 placeholder */-->
  <th:block th:replace="${links}" />

</head>
```

<br/>

##### 2. header위치에서 header fragment 호출

```html
...
<head th:replace="base :: common_header(~{::title},~{::link})">

  <title>Awesome - Main</title>

  <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/themes/smoothness/jquery-ui.css}">

</head>
...
```

<br/>

- empty fragment를 사용하여 원하는 값만 입력 가능

```html
<!--추가 링크 입력하지 않음-->
<head th:replace="base :: common_header(~{::title},~{})">

  <title>Awesome - Main</title>

</head>
```

- 적용되지 않는 값 `_` 입력하여 아무런 수정이 일어나지 않도록 가능

```html
...
<head th:replace="base :: common_header(_,~{::link})">

  <title>Awesome - Main</title>

  <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/themes/smoothness/jquery-ui.css}">

</head>
...
```

<br/>

##### 3. 적용 결과

```html
...
<head>

  <title>Awesome - Main</title>

  <!-- 공통 스트일시트와 스크립트 -->
  <link rel="stylesheet" type="text/css" media="all" href="/awe/css/awesomeapp.css">
  <link rel="shortcut icon" href="/awe/images/favicon.ico">
  <script type="text/javascript" src="/awe/sh/scripts/codebase.js"></script>

  <!-- 추가 링크 공간-->
  <link rel="stylesheet" href="/awe/css/bootstrap.min.css">
  <link rel="stylesheet" href="/awe/themes/smoothness/jquery-ui.css">

</head>
...
```

<br/>

#### 추가 사용 방법

##### 조건부 fragment 호출

```html
...
<!-- user가 admin일 경우 adminhead fragment를 삽입하고 아닐 경우 empty fragment 삽입-->
<div th:insert="${user.isAdmin()} ? ~{common :: adminhead} : ~{}">...</div>
...
```

<br/>

```html
...
<!-- user가 admin일 경우 adminhead fragment를 삽입하고 아닐 경우 아무런 수정 하지 않음-->
<div th:insert="${user.isAdmin()} ? ~{common :: adminhead} : _">
    Welcome [[${user.name}]], click <a th:href="@{/support}">here</a> for help-desk support.
</div>
...
```

<br/>

#### 실행 시 제거되는 fragment

##### th:remove

- `all`: 포함하는 태그와 모든 하위 태그 제거.
- `body`: 포함하는 태그는 제거하지 않고 모든 하위 태그 제거.
- `tag`: 포함하는 태그를 제거하지만, 하위 태그는 제거하지 않음.
- `all-but-first`: 첫 번째 태그를 제외한 모든 하위 태그를 제거.
- `none`: 아무것도 제거하지 않음. 

<br/>

#### Layout 상속

- 아래와 같이 기본 레이아웃을 작성하고, 매개 변수로 제목, 본문을 가져오는 fragment 작성

```html
<!DOCTYPE html>
<html th:fragment="layout (title, content)" xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:replace="${title}">Layout Title</title>
</head>
<body>
    <h1>Layout H1</h1>
    <div th:replace="${content}">
        <p>Layout content</p>
    </div>
    <footer>
        Layout footer
    </footer>
</body>
</html>
```

<br/>

```html
<!DOCTYPE html>
<html th:replace="~{layoutFile :: layout(~{::title}, ~{::section})}">
<head>
    <title>Page Title</title>
</head>
<body>
<section>
    <p>Page content</p>
    <div>Included on page</div>
</section>
</body>
</html>
```

- 이 경우 html 문서 내의 모든 내용은  title과 section fragment를 넣어 호출한 layout fragment로 대체 된다.

  