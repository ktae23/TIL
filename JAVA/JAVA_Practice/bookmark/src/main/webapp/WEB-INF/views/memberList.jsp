<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="ko">

<head>

  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta name="description" content="">
  <meta name="author" content="">

  <title>Bookmark4U</title>

  <!-- Bootstrap core CSS -->
  <link href="../bookmark/resources/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

  <!-- Custom styles for this template -->
  <link href="../bookmark/resources/css/shop-homepage.css" rel="stylesheet">
	
	
	<!-- 알럿 창을 아름답게 -->
 	<script src="https://unpkg.com/sweetalert/dist/sweetalert.min.js"></script>

</head>

<body>

  <!-- Navigation -->
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top">
  
    <div class="container">
      <a class="navbar-brand" href="/bookmark">Bookmark4U</a>
      
      <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive" aria-controls="navbarResponsive" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarResponsive">
        <ul class="navbar-nav ml-auto">
          <li class="nav-item active">
            <a class="nav-link" href="/bookmark">Home
              <span class="sr-only">(current)</span>
            </a>
          </li>
		 <c:if test="${not empty  cookie.logined.value || not empty cookie.logined4admin.value}">
			<li class="nav-item">
	          <a href="#" id="msgDiv" class="nav-link" ></a>
	          </li>
	          <li class="nav-item">
   	          <a href="#" id='logoutBtn' class="nav-link" onclick="window.open('../bookmark/resources/html/memberlogoutForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=200');">sign out</a>
         		</li>
			</c:if>
          <c:if test="${empty  cookie.logined.value && empty cookie.logined4admin.value }">
             <li class="nav-item">
             <a href="#" class="nav-link" onclick="window.open('../bookmark/resources/html/selectMemberByIdForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=350');">sign in</a>
          </li>
          </c:if>
        </ul>
      </div>
    </div>
  </nav>

  <!-- Page Content -->
  <div class="container">

    <div class="row">
      <div class="col-lg-3">
		<h1 class="my-4">회원 관리</h1>
		<p class="card-text">
		회원 정보를 생성, 조회, 수정, 삭제 할 수 있는 페이지입니다.
		</p>

		
        <div class="list-group">
          <a href="#" class="list-group-item" onclick="window.open('../bookmark/resources/html/memberUpdateForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=500');">회원 정보 수정</a>
          <a href="#" class="list-group-item" onclick="window.open('../bookmark/resources/html/memberRemoveForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=350');">회원 탈퇴</a>
         
        </div>

      </div>
      <!-- /.col-lg-3 -->

      <div class="col-lg-9">

        <div id="carouselExampleIndicators" class="carousel slide my-4" data-ride="carousel">
          <ol class="carousel-indicators">
            <li data-target="#carouselExampleIndicators" data-slide-to="0" class="active"></li>
            <li data-target="#carouselExampleIndicators" data-slide-to="1"></li>
            <li data-target="#carouselExampleIndicators" data-slide-to="2"></li>
          </ol>
         <div class="carousel-inner" role="listbox">
            <div class="carousel-item active">
              <img class="d-block img-fluid" src="../bookmark/resources/img/bookmark3.jpg" alt="First slide">
            </div>
            <div class="carousel-item">
              <img class="d-block img-fluid" src="../bookmark/resources/img/bookmark2.jpg" alt="Second slide">
            </div>
            <div class="carousel-item">
              <img class="d-block img-fluid" src="../bookmark/resources/img/bookmark.jpg" alt="Third slide">
            </div>
          </div>
          <a class="carousel-control-prev" href="#carouselExampleIndicators" role="button" data-slide="prev">
            <span class="carousel-control-prev-icon" aria-hidden="true"></span>
            <span class="sr-only">Previous</span>
          </a>
          <a class="carousel-control-next" href="#carouselExampleIndicators" role="button" data-slide="next">
            <span class="carousel-control-next-icon" aria-hidden="true"></span>
            <span class="sr-only">Next</span>
          </a>
        </div>

        <div class="row"  align = "center">
			 <c:if test='${!memberList.isEmpty()}'>
				<c:forEach var="member" items="${memberList }">
			          <div class="col-lg-4 col-md-6 mb-4">
			            <div class="card h-100">
			              <div class="card-body">
			                <h4 class="card-title">
			                  <a href="#">${member.name }</a>
			                </h4>
			                <p class="card-text"> id : ${member.id }</p>
			                <p class="card-text">가입일 : <fmt:formatDate value="${member.date}" pattern="yyyy.MM.dd"/></p>
			              </div>
			            </div>
			          </div>
				</c:forEach>
			</c:if>

        </div>
        <!-- /.row -->

      </div>
      <!-- /.col-lg-9 -->

    </div>
    <!-- /.row -->
 
 
  </div>
  <!-- /.container -->

  <!-- Footer -->
  <footer class="py-5 bg-dark">
    <div class="container">
      <p class="m-0 text-center text-white">Copyright &copy; ktae23's semi project</p>
    </div>
    <!-- /.container -->
  </footer>


	<script type="text/javascript" src="../bookmark/resources/js/my.js" charset="UTF-8"></script>
	
  <!-- Bootstrap core JavaScript -->
  <script src="../bookmark/resources/vendor/jquery/jquery.min.js" charset="UTF-8"></script>
  <script src="../bookmark/resources/vendor/bootstrap/js/bootstrap.bundle.min.js" charset="UTF-8"></script>

	 <!-- jQuery library -->
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js" charset="UTF-8"></script>

	<!-- Latest compiled JavaScript -->
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js" charset="UTF-8"></script>
	  


</body>
<script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-cookie/1.4.1/jquery.cookie.min.js" ></script>
<script>
$(function(){

var login=$.cookie('logined');
	$("#msgDiv").html(login);
	});
	
$(function(){

	var login=$.cookie('logined4admin');
		$("#msgDiv").html(login);
});


if (cookie.logined.value == null && cookie.logined4admin.value == null){
	$(function(){
		location.href='index';
	} 
}

</script>
</html>
