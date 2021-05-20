<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
	<div class="col-lg-12">
    	<h3 class="page-header">관리자 로그인</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-user-cog"></i>
      		</div>
      		<div class="panel-body">
				<form id="operForm" action="/admin/checkAdmin" method="post" data-toggle="validator">
	        		<div class="form-group">
	          			<label for="email" class="control-label">아이디</label> 
	          			<input class="form-control" name="email" id="email" placeholder="아이디" required data-error="아이디를 입력하세요.">
	          			<div class="help-block with-errors"></div>
	        		</div>
	        		<div class="form-group">
	          			<label for="password" class="control-label">패스워드</label> 
	          			<input class="form-control" name="password" id="password" placeholder="패스워드" required data-error="패스워드를 입력하세요." type="password">
						<div class="help-block with-errors"></div>
	        		</div>

					<button type="submit" class="btn btn-primary">확인</button>

				</form>
      		</div>
    	</div>
  	</div>
</div>





<%@include file="../includes/footer.jsp"%>
