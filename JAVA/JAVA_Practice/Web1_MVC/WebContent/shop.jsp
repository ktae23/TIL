<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="header.html" %>


<body>
<%
	String name="";
%>
<%-- HttpSession session=request.getSession(); 생략 - 내장 객체--%>
<%
	name=(String)session.getAttribute("login_name");
	if(name == null){
		name="guest";
	}
%>
	<%= name %>님 환영합니다.
	<hr>
	<form action = "Main" method = "get">
	<input tpye="hidden" name="key" value="basketInsert">
	<table>
		<tr><td><img src='img/냉장고.jfif'></td>
			<td><img src='img/TV.jfif'></td>
			<td><img src='img/세탁기.jfif'></td>
		</tr>	
		<tr><td><input type='radio' name='product' value = "냉장고"></td>
			<td><input type='radio' name='product' value = "TV"></td>
			<td><input type='radio' name='product' value = "세탁기"></td>
		</tr>	
	</table>
	<br>
	<input type="submit" value="장바구니 넣기">
	</form>

</body>
</html>