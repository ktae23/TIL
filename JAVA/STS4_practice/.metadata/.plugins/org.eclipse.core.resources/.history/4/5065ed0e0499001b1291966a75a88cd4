<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    isELIgnored="false" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="contextPath"  value="${pageContext.request.contextPath}"  />
<%
  request.setCharacterEncoding("UTF-8");
%> 

<head>
 <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-cookie/1.4.1/jquery.cookie.min.js"></script>
 
<script type="text/javascript">

 function backToList(){
	 location.href="../boardList";
 }
 
 
  function readURL(input) {
      if (input.files && input.files[0]) {
          var reader = new FileReader();
          reader.onload = function (e) {
              $('#preview').attr('src', e.target.result);
          }
          reader.readAsDataURL(input.files[0]);
      }
  }  
</script> 
<title>답글쓰기 페이지</title>
</head>

<body>
<div class="container">
 <h1>${parentNO }번 글의 답글쓰기</h1>
  <form name="frmReply" method="post"  action="../boardWrite"   enctype="multipart/form-data">
  <input type="hidden" name="parentNO" value="${parentNO }">
    <table class="table table-border">
    <tr>
			<td align="right"> 작성자 :&nbsp; </td>
			<td><input type="text" size="20" maxlength="100"  name="writer"></input> </td>
		</tr>
		<tr>
			<td align="right">제목 :&nbsp;  </td>
			<td><input type="text" size="62"  maxlength="500" name="title"> </input></td>
		</tr>
		<tr>
			<td align="right" valign="top"><br>내용 :&nbsp; </td>
			<td><textarea name="content" rows="10" cols="65" maxlength="4000"> </textarea> </td>
		</tr>
		<tr>
			<td align="right">비밀번호 :&nbsp;  </td>
			<td><input type="password" size="20" maxlength="12" name="passwd"> </input> </td>
		</tr>
		<tr>
			<td align="right">파일 첨부 :  </td>
			<td> <input type="file" name="file"  onchange="readURL(this);" /></td>
            <td></td>
		</tr>
		<tr>
			<td align="right"> </td>
			<td>
				<input type=submit value="답글쓰기" class="btn btn-info"/>
				<input type=button value="취소"onClick="backToList()" class="btn btn-warning"/>
				
			</td>
		</tr>
    
    </table>
  
  </form>
  </div>
</body>
</html>
