<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<table border="1" align="center" width="100%">
		<tr align="center" bgcolor="lightgreen">
			<th>아이디</th>
			<th>비밀번호</th>
			<th>이름</th>
			<th>가입일</th>
		</tr>
		<tr align = "center">
			<th>${member.id }</th>
			<th>${member.pw }</th>
			<th>${member.name }</th>
			<th>${member.date }</th>
		</tr>
	</table>
</body>
</html>