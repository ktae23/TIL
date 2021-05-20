<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
	<div class="col-lg-12">
    	<h3 class="page-header">신청확인</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-user-check"></i>
      		</div>
      		<div class="panel-body">
				<form id="operForm" action="/regist/checkUser" method="post" data-toggle="validator">

         			<div class="form-group">
           				<label for="email" class="control-label">이메일</label>
           				<input class="form-control" name="email" id="email" placeholder="이메일" type="email" required data-error="이메일을 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>
 					<div class="form-group">
           				<label for="password" class="control-label">패스워드</label>
           				<input class="form-control" name="password" id="password" placeholder="패스워드" type="password" required data-error="패스워드를 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>

					<button type="submit" class="btn btn-primary">확인</button>

				</form>
      		</div>
    	</div>
  	</div>
</div>





<%@include file="../includes/footer.jsp"%>
