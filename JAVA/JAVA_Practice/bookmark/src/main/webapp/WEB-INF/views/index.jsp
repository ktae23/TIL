<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
			<c:if test="${not empty  cookie.logined.value || not empty  cookie.logined4admin.value}">
				<li class="nav-item">
					<a href="#" id="msgDiv" class="nav-link" ></a>
				</li>
				<li class="nav-item">
					<a href="#" id='logoutBtn' class="nav-link" onclick="window.open('../bookmark/resources/html/memberlogoutForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=200');">sign out</a>
				</li>
			</c:if>
			<c:if test="${ empty  cookie.logined.value && empty  cookie.logined4admin.value}">
    	         <li class="nav-item">
					<a href="#" class="nav-link" onclick="window.open('../bookmark/resources/html/selectMemberByIdForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=350');">sign in</a>
				</li>
				<li class="nav-item">
					<a href="#" class="nav-link" onclick="window.open('../bookmark/resources/html/memberInsertForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=500');">sign up</a>
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
		<h1 class="my-4">북마크 게시판</h1>

		<p class="card-text">
		폴더 형태의 북마크가 아닌,
		게시판 형태의 북마크입니다.
		</p>
		<p class="card-text">
		짧은 설명을 첨부 할 수 있어서
		어떤 링크였는지 한눈에 파악 할 수 있죠.
		</p>
        <div class="list-group">
        <c:if test="${not empty  cookie.logined.value || not empty cookie.logined4admin.value}">
	      	<a href="memberList" class="list-group-item">회원 관리</a>
          	<a href="bookmarkList" class="list-group-item">북마크 게시판</a>
        </c:if>
     
        <c:if test="${empty  cookie.logined.value && empty cookie.logined4admin.value }">
         	<a href="" class="list-group-item" onclick="window.open('../bookmark/resources/html/selectMemberByIdForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=350');">회원 관리</a>
          	<a href="" class="list-group-item" onclick="window.open('../bookmark/resources/html/selectMemberByIdForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=400,height=350');">북마크 게시판</a>
        </c:if>
    	<c:if test="${not empty cookie.logined4admin.value}">
	         <a href="memberList4admin" class="list-group-item">관리자용 회원 관리 페이지</a>
	    </c:if>
          
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

        <div class="row">

          <div class="col-lg-4 col-md-6 mb-4">
            <div class="card h-100">
              <div class="card-body">
                <h4 class="card-title">
                  <a href="https://www.naver.com">Naver</a>
                </h4>
                <p class="card-text">네이버 북마크!</p>
                 <p class="card-text">작성자 : 관리자</p>
                <p class="card-text">작성일 : 2021.03.17</p>
              </div>
            </div>
          </div>

          <div class="col-lg-4 col-md-6 mb-4">
            <div class="card h-100">
              <div class="card-body">
                <h4 class="card-title">
                  <a href="https://www.google.com">Google</a>
                </h4>
                <p class="card-text">구글 북마크!</p>
                <p class="card-text">작성자 : 관리자</p>
                <p class="card-text">작성일 : 2021.03.17</p>
              </div>
            </div>
          </div>

          <div class="col-lg-4 col-md-6 mb-4">
            <div class="card h-100">
              <div class="card-body">
                <h4 class="card-title">
                  <a href="https://www.multicampus.com">Multi Campus</a>
                </h4>
                <p class="card-text">멀티캠퍼스 북마크!</p>
                <p class="card-text">작성자 : 관리자</p>
                <p class="card-text">작성일 : 2021.03.17</p>
              </div>
            </div>
          </div>


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


	


</body>
	<script type="text/javascript" src="../bookmark/resources/js/my.js"></script>
	
  <!-- Bootstrap core JavaScript -->
  <script src="../bookmark/resources/vendor/jquery/jquery.min.js"></script>
  
  <script src="../bookmark/resources/vendor/bootstrap/js/bootstrap.bundle.min.js"></script>

	 <!-- jQuery library -->
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>

	<!-- Latest compiled JavaScript -->
	<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>
	  
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
</script>

</html>
