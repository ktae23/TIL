<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"
    isELIgnored="false" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>    
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="contextPath"  value="${pageContext.request.contextPath}"  />
<%
  request.setCharacterEncoding("UTF-8");
%>  
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-cookie/1.4.1/jquery.cookie.min.js"></script>
 <style>
   .cls1 {text-decoration:none;}
   .cls2{text-align:center; font-size:30px;}
  </style>
  <meta charset="UTF-8">
  <title>글목록창</title>
</head>
<script>
	function fn_articleForm(articleForm){
		var login=$.cookie('logined');
	  if(login){
	    location.href=articleForm;
	  }else{
	    alert("로그인 후 글쓰기가 가능합니다.")
	    window.close();
	  }
	}
</script>
<body>
<br><br><br>
<div class="container">
<table align="center" border="0"  width="80%"  class="table table-striped">
  <tr height="10" align="center"  bgcolor="lightgray">
     <td width="10%">글번호</td>
     <td >작성자</td>              
     <td >제목</td>
     <td >작성일</td>
  </tr>
<c:choose>
  <c:when test="${articlesList ==null }" >
    <tr  height="10">
      <td colspan="4">
         <p align="center">
            <b><span style="font-size:9pt;">등록된 글이 없습니다.</span></b>
        </p>
      </td>  
    </tr>
  </c:when>
  <c:when test="${articlesList !=null }" >
    <c:forEach  var="article" items="${articlesList }" varStatus="articleNum" >
     <tr align="center">
	<td width="5%">${articleNum.count}</td>
	<td width="10%">${article.id }</td>
	<td align='left'  width="35%">
	  <span style="padding-right:30px"></span>
	   <c:choose>
	      <c:when test='${article.level > 1 }'>  
	         <c:forEach begin="1" end="${article.level }" step="1">
	              <span style="padding-left:20px"></span>    
	         </c:forEach>
	         <span style="font-size:12px;">[답변]</span>
                   <a class='cls1' href="../viewArticle?articleNO=${article.articleNO}">${article.title}</a>
	          </c:when>
	          <c:otherwise>
	            <a class='cls1' href="../viewArticle?articleNO=${article.articleNO}">${article.title }</a>
	          </c:otherwise>
	        </c:choose>
	  </td>
	  <td  width="30%">${article.writeDate}</td> 
	</tr>
    </c:forEach>
     </c:when>
    </c:choose>
</table>
</div>
<!-- <a  class="cls1"  href="#"><p class="cls2">글쓰기</p></a> -->
<center>
<a    href="javascript:fn_articleForm('../boardWriteForm')"><p class="btn btn-info">글쓰기</p></a>
</center>
</body>
</html>
