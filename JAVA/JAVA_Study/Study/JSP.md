## JSP (Java Server Pasges)

- `Dynamic Web Project` - `WebContent` - `JSP file`
- HTML 내부에서 프로그램 코드를 돌리기 위해 사용
- JSP는 Servlet으로 변환하여 동작 함

<br/>

#### JSP templete

- html, text, css, js, xml

#### JSP element ( Scripting / tag)

- Scripting : 
  - `<%@` [코드] `%>` : `지시자` page 지시자 등 정보를 지시하기 위해 사용
    - page : lnaguage, contentType, import...+
    - include : 주로 정적 파일을 포함(html, txt, jsp...+)
    - taglib : prepix를 만나면 uri로 가서 처리하라는 커스텀 태그 처리 지시
  - `<%!` [코드] `%>` : `선언문` 전역변수 선언 및 메소드 선언에 사용
  - `<%` [코드] `%>` : `Scriptlet` 자바 코드를 입력할 수 있는 부분
  - `<%=` [코드] `%>` :  `표현식` 응답 결과에 넣고 싶은 자바 코드 입력(화면에 출력할 내용)
- Tag : 
  - `<jsp:액션 속성=값` `</jsp:액션>` : Standard tag
  - `<[prepix] <%=` [코드] `%>` `</[prepix]>` : Custrom tag

<br/>

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="header.html" %>
<%-- <%@ taglib prefix="jes" uri="..." %>--%>
    
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	${name }님 환영합니다.
	<br><a href='shop.html'>쇼핑 가기</a>
	<jsp:include page="img.html"></jsp:include>
	<!-- <pkt:<%--<%=  %>--%></pkt:> -->
	<br>
	<%!
	int i=10;
	public void printI() {
		System.out.println(i);
	}
	%>
	
	<%=i++ %>
	<% 
		printI();
	%>
</body>
</html>



```

<br/>

```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>

</body>
</html>
```

<br/>

- 마이크로소프트에서 ASP(Active Server Page)라는 웹을 쉽게 개발할 수 있는 스크립트 엔진을 발표함(1998년)
  - 1997년에 발표된 서블릿은 상대적으로 개발방식이 불편하여 이에 대항하기 위해 1999년 썬마이크로시스템즈에서  JSP를 발표 함
  - JSP는 항상 서블릿으로 변경 되어 사용 됨

<br/>

### JSP 라이프 사이클

- JSP 위치 : `workspace` - `.metadata` - `.plugins` - `org.eclipse.wst.server.core` - `temp0` - `wtpwebapps` - `프로젝트` 

- JSP가 변환 된 Servlet 위치 : `workspace` - `.metadata` - `.plugins` - `org.eclipse.wst.server.core` - `work` - `caltalina`- `localhost` - `프로젝트` - `org` - `apache` - `jsp`
  - JSP가 `.java`와 `.class` 두 개로 저장 
  - `.java` 파일 내부의 `_jspService()` 메서드 내에 JSP 파일 내용이 변환되어 들어가 있음

<br/>

### JSP의 실행순서

1. 브라우저가 웹서버에 JSP에 대한 요청 정보를 전달한다.
2. 브라우저가 요청한 JSP가 최초로 요청했을 경우만 JSP로 작성된 코드가 서블릿으로 코드로 변환한다. (java 파일 생성)
3. 서블릿 코드를 컴파일해서 실행가능한 bytecode로 변환한다. (class 파일 생성)
4. 서블릿 클래스를 로딩하고 인스턴스를 생성한다.
5. 서블릿이 실행되어 요청을 처리하고 응답 정보를 생성한다.

<br/>

### 주석

- 각 위치, 즉 처리 순서에 따라 주석이 나타나는 위치가 다름
- HTML 주석 :은 제일 먼저 사라지고 그 다음 JSP, 그 다음 JAVA 순으로 실행, 변환 되면서 주석 처리 됨

<br/>

- **HTML 주석**	
  - 페이지를 웹에서 화면에 보여 줄때 주석 처리 되어 표현되지 않음

```html
<!-- HTML 주석 -->
```

<br/>

- **JSP 주석**	
  - JSP 페이지에서만 사용, 자바로 변환 되지 않음
  - 웹 브라우저에서 소스 보기를 해도 표시 되지 않음

```jsp
<%-- JSP 주석 --%>
```

<br/>

- **JAVA 주석**
  - JSP가 자바로 바뀔때까지는 변환 되나 Servlet으로 실행 될 때는 표현 되지 않음

```java
// JAVA  주석
/*
JAVA 주석
*/
```

<br/>

### JSP 내장 객체

- JSP에 입력한 대부분의 코드는 생성되는 서블릿 소스의 _jspService() 메소드 안에 삽입 됨
  - 해당 코드에 미리 선언 된 객체들이 있는데 해당 객체들은 JSP에서도 바로 사용 가능하다
  - response, request, application, session, out 과 같은 변수를 내장 객체라 함

<br/>

![JSP 내장객체 종류](https://www.boostcourse.org/web326/lecture/58961/?isDesc=false#)

