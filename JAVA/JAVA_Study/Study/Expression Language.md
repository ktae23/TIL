## Expression Language

### 표현 언어

- 값을 표현하는 데 사용되는 스크립트 언어로서 JSP의 기본 문법을 보완하는 역할
  - JSP의 스코프에 맞는 속성 사용
  - 집합 객체에 대한 접근 방법 제공
  - 수치 연산, 관계 연산, 논리 연산자 제공
  - 자바 클래스 메소드 호출 기능 제공
  - 표현언어만의 기본 객체 제공

<br>

```jsp
${ 출력부}
<%--JSP의스크립트요소( 트스킵트릿, 표현식, 선언부)를 제외한 나머지 부분에서 사용 될 수 있음 -->
```

<br>

### EL의 기본 객체 

![EL의 기본 객체](https://cphinf.pstatic.net/mooc/20180130_153/1517281495386qOuqH_PNG/2_6_1__.PNG)

<br>

### EL의 데이터 타입

- 불리언 타입
  - true / false
- 정수 타입
  - 0~9로 이루어진 정수 값
  - 음수일 경우 `-`가 붙음
- 실수 타입
  - 0~9로 이루어져 있으며 소수점 `.` 사용 가능
  - 3.23e3과 같이 지수형으로 표현 가능
- 문자열 타입
  - 따옴표(`'`또는`"`)로 둘러 싼 문자열
  - 작은 따옴표를 사용해서 표현 할 경우 값에 포함 된 작은 따옴표는 `\`와 같이 `\`(백슬래시) 기호와 함게 사용
  - `\` 기호 자체는 `\\`로 표시
- 널 타입
  - null

<br>

### 객체 접근 규칙

```jsp
${<표현1>.<표현2>}
```

- 표현 1이나 표현 2가 null이면 null 값 반환
- 표현 1이 MAP이면 표현2를 key로 하는 값을 반환
- 표현 1이 List 나 배열이면 표현2가 정수일 경우 해당 정수번째 index에 해당하는 값 반환
  - 정수가 아닐 경우 오류 발생
- 표현 1이 객체일 경우 표현 2에 해당하는 getter메소드에 해당하는 메소드(ex : getName())를 호출한 결과 반환

## EL의 수치 연산자

- 덧셈 : `+`
- 뺄셈 : `-`
- 곱셈 : `*`
- 나눗셈 : `/` 또는 `div`
- 나머지 : `%` 또는 `mod`

<br>

- 숫자가 아닌 객체와 수치 연산자를 사용 할 경우 객체를 숫자 값으로 변환 후 연산자 수행

```jsp
${"10"+1} -> ${10+1}
```

<br>

- 숫자로 변환 할 수 없는 객체와 수치 연산자를 함께 사용하면 에러 발생

```jsp
${"열"+1} -> <%--Error-->
```

<br>

- 수치 연산자에서 사용되는 객체가 null이면 0으로 처리

```jsp
${null+1} -> ${0+1}
```

<br>

### 비교 연산자

- `==` 또는 `eq`
- `!=` 또는 `ne`
- `<` 또는 `lt`
- `>` 또는 `gt`
- `<=` 또는 `le`
- `>=` 또는 `ge`
- 문자열 비교
  - `${str=='값'}`
  - `str.compareTo("값" == 0)` 과 동일

<br>

### 논리 연산자

- `&&` 또는 `and`
- `||` 또는 `or`
- `!` 또는 `not`

<br>

### empty 연산자, 비교 선택 연산자

```jsp
empty <값>
```

- <값>이 null이면 true를 리턴
- <값>이 빈 문자열("")이면 true를 리턴
- <값>이 길이가 0인 배열이면 true를 리턴
- <값>이 빈 Map이면 true를 리턴
- <값>이 빈 Collection이면 true를 리턴
- 이 외의 경우에는 false를 리턴

<br>

```jsp
<수식> ? <값1> : <값2>
```

- <수식>의 결과 값이 ture면 <값1>을 리턴
  - false면 <값2>를 리턴

<br>

### 연산자 우선순위

1. `[] .`
2. `()`
3. `- (단일) not ! empty`
4. `* / div % mod`
5. `+ -`
6. `< > <= >= lt gt le ge`
7. `== != eq ne`
8. `&& and`
9. `|| or`
10. `? :`

<br>

### 표현 언어 비활성화 : JSP에 명시하기

```jsp 
<%@ page isELIgnored = "true" %>
```

<br>

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
    pageContext.setAttribute("p1", "page scope value");
    request.setAttribute("r1", "request scope value");
    session.setAttribute("s1", "session scope value");
    application.setAttribute("a1", "application scope value");
%>    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
pageContext.getAttribute("p1") : ${pageScope.p1 }<br>
request.getAttribute("r1") : ${requestScope.r1 }<br>
session.getAttribute("s1") : ${sessionScope.s1 }<br>
application.getAttribute("a1") : ${applicationScope.a1 }<br>
<br><br>
pageContext.getAttribute("p1") : ${p1 }<br>
request.getAttribute("r1") : ${r1 }<br>
session.getAttribute("s1") : ${s1 }<br>
application.getAttribute("a1") : ${a1 }<br>

</body>
</html>
```

- 같은 변수명일 경우 작은 스코프부터 찾아내지만 가독성과 정확성을 위 해 참조하여 변수 값을 주는 것이 좋음