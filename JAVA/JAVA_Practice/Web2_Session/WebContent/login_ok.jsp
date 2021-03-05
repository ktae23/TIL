<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<%
	    
		String id=(String)session.getAttribute("login_id");
		System.out.println(id+"의 세션:"+session.getId());
	%>
	<%= id %>님 로그인 되셨습니다.
	<br>
	<form action="main">
		<input type="hidden" name="sign" value="logout">
		<input type="submit" value="로그아웃">
	
	</form>

</body>
</html>