<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>SW 마에스트로 해커톤 대회</title>

	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
	
	<link href="/resources/css/common.css" rel="stylesheet">
	
    <!-- Bootstrap Core CSS -->
    <link href="/resources/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <!-- MetisMenu CSS -->
    <link href="/resources/vendor/metisMenu/metisMenu.min.css" rel="stylesheet">

    <!-- DataTables CSS -->
    <link href="/resources/vendor/datatables-plugins/dataTables.bootstrap.css" rel="stylesheet">

    <!-- DataTables Responsive CSS -->
    <link href="/resources/vendor/datatables-responsive/dataTables.responsive.css" rel="stylesheet">

    <!-- Custom CSS -->
    <link href="/resources/dist/css/sb-admin-2.css" rel="stylesheet">

    <!-- Custom Fonts -->
    <link href="/resources/vendor/font-awesome/css/all.css" rel="stylesheet" type="text/css">
    
	<script>
		<c:if test="${null ne result && !empty result.msg}">
			alert("<c:out value="${result.msg}"/>");
		</c:if>
		<c:if test="${null ne result && !empty result.url}">
			location.href = "<c:out value="${result.url}"/>";
		</c:if>
	</script>
</head>

<body>

<div id="wrapper">

		<%-- LAB Command Injection : Insecure Code --%>
		<script type="text/javascript">
			$(document).ready(function(e) {
				setInterval(function() {
					$.ajax({
			            url: '/getDateTimeAction?cmd=date /t',
			            processData: false, 
			            contentType: false,
			            type: 'GET',
			            success: function(result) {
			                $('.now').html('<i class="fas fa-clock"></i> '+result);
			            }
			        });
				}, 3000);
			});	
		</script>   
		
		<!-- Navigation -->
    	<nav class="navbar navbar-default navbar-static-top" role="navigation" style="margin-bottom: 0">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/">SW 마에스트로 해커톤 대회</a>
            </div>

            <div class="navbar-default sidebar" role="navigation">
                <div class="sidebar-nav navbar-collapse">
                    <ul class="nav" id="side-menu">
                   		<c:if test="${!empty SESS_USER_NAME}">
                    	<li>
                   			<p style="padding:10px 0 0 10px;">
                    			<a href="/regist/get"><b><c:out value="${SESS_USER_NAME}"/></b> [<c:out value="${SESS_USER_ROLE}"/>]</a>님 환영합니다.
                   			</p>
                    	</li>
                    		</c:if>
                    	<li>
                    		<a href="/" class="now"><i class="fas fa-clock"></i> </a>
                    	</li>
                        <li>
                            <a href="/eventOverview"><i class="fas fa-globe"></i> 행사개요</a>
                        </li>
<c:if test="${SESS_USER_ROLE ne 'ADMIN'}">
                        <li>
                            <a href="/board/list"><i class="fas fa-bullhorn"></i> 공지사항</a>
                        </li>
                        <li>
                            <a href="/regist/register"><i class="fas fa-edit"></i> 참가신청</a>
                        </li>
                        <li>
                            <a href="/regist/checkUser"><i class="fas fa-user-check"></i> 신청확인</a>
                        </li>
<%--
	// TODO TEST
	1) 정답제출 메뉴 추가
	2) 로그인한 사용자에게만 정답제출 메뉴를 노출
	3) 정답제출 메뉴를 클릭하면 /answer/answer 링크로 이동 
--%>                        


</c:if>                        
<c:if test="${SESS_USER_ROLE eq 'ADMIN'}">
                        <li>
                            <a href="/admin/boardList"><i class="fas fa-th-list"></i> 공지관리</a>
                        </li>
                        <li>
                            <a href="/admin/registList"><i class="fas fa-chart-bar"></i> 신청현황</a>
                        </li> 						
</c:if>
<c:if test="${empty SESS_USER_ROLE}">
						<li>
							<a href="/admin/checkAdmin"><i class="fas fa-user-cog"></i> 관리자 로그인</a>
						</li>
</c:if>				
<c:if test="${!empty SESS_USER_ROLE}">
                        <li>
                            <a href="/logout"><i class="fas fa-sign-out-alt"></i> 로그아웃</a>
                        </li>
</c:if>                  

                    </ul>
                </div>
            </div>
        </nav>

        <div id="page-wrapper">
        
        