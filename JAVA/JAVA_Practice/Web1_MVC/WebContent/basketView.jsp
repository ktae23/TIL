<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="java.util.ArrayList"  %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<%
		ArrayList<String> list=(ArrayList<String>)session.getAttribute("basket");
		for(String product:list){
			out.write(product+"<br>");
		}
	%>
</body>
</html>