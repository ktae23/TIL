<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
	<div class="col-lg-12">
    	<h3 class="page-header">신청현황</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-chart-bar"></i>
      		</div>
      		<div class="panel-body">
				
				
				<div class="form-group">
          			<label>이름</label> 
          			<input class="form-control" name="name" value="<c:out value="${regist.name}"/>" readonly="readonly">
				</div>
        		<div class="form-group">
          			<label>이메일</label> 
          			<input class="form-control" name="email" value="<c:out value="${regist.email}"/>" readonly="readonly">
        		</div>
        		<div class="form-group">
          			<label>연락처</label> 
          			<input class="form-control" name="phone" value="<c:out value="${regist.phone}"/>" readonly="readonly">
        		</div>
        		<div class="form-group">
          			<label>하고싶은 말</label>
          			<textarea class="form-control" rows="3" name="content" readonly="readonly">${regist.content}</textarea>
        		</div>
				<div class="form-group">
				  	<label>등록일</label> 
				  	<input class="form-control" name="regDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${regist.regDate}"/>" readonly="readonly">            
				</div>
				<div class="form-group">
				  	<label>수정일</label> 
				  	<input class="form-control" name="updateDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${regist.updateDate}"/>" readonly="readonly">            
				</div>

				<form id="operForm" action="/admin/registList" method="get">
					<input type="hidden" name="rgno"    value="<c:out value="${regist.rgno}"/>">
					<input type="hidden" name="pageNum" value="<c:out value="${cri.pageNum}"/>">
					<input type="hidden" name="amount"  value="<c:out value="${cri.amount }"/>">
					<input type="hidden" name="keyword" value="<c:out value="${cri.keyword}"/>">
					<input type="hidden" name="type"    value="<c:out value="${cri.type   }"/>">  
					<button type="submit" class="btn btn-info">목록으로</button>
				</form>
      		</div>
    	</div>
  	</div>
</div>

<div class="row">
	<div class="col-lg-12">
		<div class="panel panel-default">
			<div class="panel-heading"><i class="fas fa-file-upload"></i> 첨부파일</div>
			<div class="panel-body">
				<div class='uploadResult'> 
					<ul></ul>
				</div>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">
	$(document).ready(function() {
		(function() {
			var rgno = '<c:out value="${regist.rgno}"/>';    	
			$.getJSON('/regist/getAttachList', { rgno : rgno }, function(arr) {
				var str = '';
				$(arr).each(function(i, attach) {
					
					var fileCallPath = encodeURIComponent(attach.uploadPath + "/" + attach.fileName);
					str += '<li data-path="' + attach.uploadPath + '" data-uuid="' + attach.uuid + '" data-filename="' + attach.fileName + '" data-type="' + attach.fileType + '">';
					str += '<div>';
					str += '<span>' + attach.fileName + '</span><br/>';
					str += '<a href="/download?fileName=' + fileCallPath + '">';
					if (attach.fileType) {
						str += '<img src="/display?fileName=' + fileCallPath + '">';
					} else {
						str += '<img src="/resources/img/attach.png"></a>';	
					}
					str += '</a>';
					str += '</div>';
					str += '</li>';

				});
				$('.uploadResult ul').html(str);
			});
		})();

 	});
</script>


<%@include file="../includes/footer.jsp"%>
