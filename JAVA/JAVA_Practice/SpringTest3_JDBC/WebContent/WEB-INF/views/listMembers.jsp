<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"  %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<table  border="1" align="center" width="80%">
		<tr align="center" bgcolor="lightgreen">
		<th>아이디</th><th>비밀번호</th><th>이름</th><th>가입일</th>
		</tr>
		<c:forEach var="m" items="${membersList }">
			<tr>
				<td>${m.id }</td>
				<td>${m.pw }</td>
				<td>${m.name }</td>
				<td>${m.date }</td>
			</tr>
		</c:forEach>
	</table>
</body>
</html>