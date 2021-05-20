<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
	<div class="col-lg-12">
    	<h3 class="page-header">정답제출</h3>
  	</div>
</div>

<%-- TODO XSS Secure coding
		서버에서 생성된 데이터를 태그 라이브러리를 이용하여 필터링하여 출력
--%>
<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-user-check"></i>
      		</div>
      		<div class="panel-body">
        		<div class="form-group">
          			<label>하고싶은 말</label>
          			<textarea class="form-control" rows="3" name="content" readonly="readonly">${answer.content}</textarea>
        		</div>
				<div class="form-group">
				  	<label>등록일</label> 
				  	<input class="form-control" name="regDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${answer.regDate}"/>" readonly="readonly">            
				</div>
				<div class="form-group">
				  	<label>수정일</label> 
				  	<input class="form-control" name="updateDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${answer.updateDate}"/>" readonly="readonly">            
				</div>
				
				<form id="operForm" action="/answer/modify" method="GET">
					<button type="submit" class="btn btn-primary">수정</button>
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
			var asno = '${answer.asno}';    	
			$.getJSON('/answer/getAttachList', { asno : asno }, function(arr) {
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
