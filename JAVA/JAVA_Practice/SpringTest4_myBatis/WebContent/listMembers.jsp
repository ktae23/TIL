<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<table border="1">
		<tr>
			<th>no</th><th>id</th><th>pw</th><th>name</th><th>date</th>
		</tr>

		<c:forEach var="m" items="${memberList }" varStatus="i">
			<tr>
				<td>${i.count }</td>
				<td>${m.id }</td>
				<td>${m.pw }</td>
				<td>${m.name }</td>
				<td>${m.date }</td>
			</tr>
		</c:forEach>
	</table>
</body>
</html>