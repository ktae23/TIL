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
	$(document).ready(function(){
		$("#loginP").click(function(){//로그인 처리	
					
					var id=$("#id_").val();
					var pw=$("#pw_").val();
					
					//alert(id+":"+pw);		
					
					$.post("login.jes",
						  {			   
						    id:id,
						    pw:pw
						  },
						  function(data, status){	
							  var obj=JSON.parse(data);			  
							  	if(obj.name){
							  		data = obj.name+"<input type='button' value='logout' id='logoutBtn' class='btn btn-primary'>";	
							  		$.cookie("logined",data);	    
									$("#msgDiv").html(data );
									location.href="../boardList";
								}else{
									alert(obj.msg);
									location.reload();	
								}							   
						  }//end function
					);//end post() 
				});//end 로그인 처리
		
	});
</script>
<body>
<br><br><br>
<div class="container">
     <div class="form-group">
                        <label for="InputEmail">ID</label>
                        <input type="text" class="form-control" id="id_" placeholder="ID를 입력해주세요">
                    </div>
                    <div class="form-group">
                        <label for="inputPassword">비밀번호</label>
                        <input type="password" class="form-control" id="pw_" placeholder="비밀번호를 입력해주세요">
                    </div>
<p class="btn btn-info" id="loginP">로그인</p>
</div>
</body>
</html>
