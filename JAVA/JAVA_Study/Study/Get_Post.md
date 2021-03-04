#### Get 방식

- 서블릿에 데이터를 전송 할 때 데이터가 URL 뒤에 name = value 형태로 전송 됨
- 여러 개의 데이터를 전송 할 때는 `&`로 구분해서 전송 됨
- 보안이 취약
- 최대 255자까지 전송 가능
- 기본 전송 방식으로 사용이 쉬움
- 웹 브라우저에 직접 입력해서 전송할 수 있음
- 서블릿에서 doGet()을 이용해 데이터 처리

<br/>

#### Post 방식

- 서블릿에 데이터를 전송 할 때는 TCP/IP 프로토콜 데이터의 HEAD 영역에 숨겨진 채 전송 됨
- 보안에 유리
- 전송 데이터 용량 무제한
- 전송 시 서블릿에서 또 다시 가져오는 작업을 해야 하므로 처리 속도가 Get 보다 느림
- 서블릿에서는 doPost()를 이용해 데이터를 처리
- Body 영역에 데이터를 숨겨서 전송

<br/>

![](http://www.icodeguru.com/dotnet/core.c.sharp.and.dot.net/0131472275/images/0131472275/graphics/17fig01.gif)

<br/>

#### 두 가지 방식 모두 처리하기

```java
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
     처리 코드   
    }
```

<br/>

#### 자바스크립트로 서블릿에 요청하기

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>로그인</title>
<script type="text/javascript">
	function fn_validate() {
		// document -> 자바스크립트 내장 객체
		// <form> 태그의 name 속성으로 <form> 태그를 받아 옴
        // 필드 체크 목적
		var frmLogin = document.frmLogin;
		
		// <form> 태그 내의 <input> 태그의 name 속성으로 입력한 ID와 비밀번호를 받아 옴
		var user_id = frmLogin.id.value;
		var user_pw = frmLogin.pw.value;
		
		if ((user_id.length == 0 || user_id == "") || (user_pw.length == 0 || user_pw == "")){
			alert("아이디와 비밀번호는 필수입니다.");
		}else{
			frmLogin.method = "post";	// <form> 태그 전송 방식 post로 설정
			frmLogin.action = "main";	// action 속성을 서블릿 매핑 이름으로 설정
			frmLogin.submit();	//자바스크립트에서 서블 릿으로 전송
		}
	}
</script>

</head>
<body>
	<form name="frmLogin" method="post" action="main" encType="UTF-8">
		<input type="hidden" name="sign" value="login">
		ID <input type="text" name="id"><br>
		PW <input type="password" name="pw"><br>
		<input type="button" onClick="fn_validate()" value="로그인">	<!-- 로그인 버튼을 누르면 자바스크립트의 함수 실행 -->
		<input type="reset" value="다시 입력">
	</form>
</body>
</html>
```

