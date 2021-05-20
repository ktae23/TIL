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
				<form role="form" id="frmModify" action="/answer/modify" method="post" data-toggle="validator">
					<input type="hidden" name="asno" value="${answer.asno}">
									 
         			<div class="form-group">
           				<label for="content" class="control-label">하고싶은 말</label>
           				<textarea class="form-control" rows="3" name="content" id="content" placeholder="하고싶은 말">${answer.content}</textarea>
         			</div>
					
					<button type="submit" data-oper="modify" class="btn btn-primary">수정</button>
				  	<button type="submit" data-oper="remove" class="btn btn-danger" >삭제</button>
				  	<button type="button" data-oper="get"    class="btn btn-info"   >취소</button>
				</form>
				<form role="form" id="frmGoGet" action="/answer/get" method="post">
					<input type="hidden" name="asno" value="${answer.asno}">
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
				<div class="form-group uploadDiv">
					<input type="file" name="uploadFile" multiple>
				</div>
				<div class="uploadResult">
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
				var str = "";
				$(arr).each(function(i, attach) {
					var fileCallPath = encodeURIComponent(attach.uploadPath + '/' + attach.fileName);
					str += '<li data-path="' + attach.uploadPath + '" data-uuid="' + attach.uuid + '" data-filename="' + attach.fileName + '" data-type="' + attach.fileType + '">';
					str += '<div>';
					str += '<span>' + attach.fileName + '</span>';
					str += '<button type="button" data-file="' + fileCallPath + '" data-type="image" class="btn btn-warning btn-circle"><i class="fas fa-times"></i></button><br>';
					str += '<a href="/download?fileName=' + fileCallPath + '">';
					if (attach.fileType) {
						str += '<img src="/display?fileName=' + fileCallPath + '">';
					} else {
						str += '<img src="/resources/img/attach.png">';	
					}
					str += '</a>';
					str += '</div>';
					str += '</li>';
				});
				$('.uploadResult ul').html(str);
			});
		})();
  
  
		$('.uploadResult').on('click', 'button', function(e) {
			if (confirm('파일을 삭제하시겠습니까?')) {
				var targetLi = $(this).closest('li');
				targetLi.remove();
			}
		});  

		<%-- TODO File upload secure coding
				정규식을 이용하여 파일 확장자가  exe, sh, zip, alz 인지를 확인 --%>
		var regex;
		<%-- TODO File upload secure coding
				파일 크기를 5MB로 제한 --%>
		var maxSize = 0;
		
		function checkExtension(fileName, fileSize) {
			<%-- TODO File upload secure coding 
				1) 파일의 크기를 체크
				2) 파일의 종류를 체크
			--%>
		    
		    return true;
		}

		$('input[type="file"]').change(function(e) {
			var formData = new FormData();
			var inputFile = $('input[name="uploadFile"]');
			var files = inputFile[0].files;
			for (var i = 0; i < files.length; i ++) {
				if (!checkExtension(files[i].name, files[i].size)) {
					return false;
				}
				formData.append('uploadFile', files[i]);
			}

			$.ajax({
				url: '/uploadAjaxAction',
				processData: false, 
				contentType: false,
				data: formData,
				type: 'POST',
				dataType: 'json',
				success: function(result) {
					showUploadResult(result); 
				}
			});
		});    

		function showUploadResult(uploadResultArr) {
			if (!uploadResultArr || uploadResultArr.length == 0) { 
				return; 
			}

			var uploadUL = $('.uploadResult ul');
			var str = '';
	        $(uploadResultArr).each(function(i, obj) {
	        	var fileCallPath = encodeURIComponent(obj.uploadPath + '/' + obj.fileName);
				str += '<li data-path="' + obj.uploadPath + '" data-uuid="' + obj.uuid + '" data-filename="' + obj.fileName + '" data-type="' + obj.image + '">';
				str += '<div>';
				str += '<span>' + obj.fileName + '</span>';
				str += '<button type="button" data-file="' + fileCallPath + '" data-type="image" class="btn btn-warning btn-circle"><i class="fas fa-times"></i></button><br>';
				str += '<a href="/download?fileName=' + fileCallPath + '">';
				if (obj.image) {
					str += '<img src="/display?fileName=' + fileCallPath + '">';
				} else {
					str += '<img src="/resources/img/attach.png">';
				}
				str += '</a>';
				str += '</div>';
				str += '</li>';
	        });
			uploadUL.append(str);
		}
	});
</script>


<script type="text/javascript">
	$(document).ready(function() {
		var formObj = $('#frmModify');
		$('button').on('click', function(e) {
	    	e.preventDefault(); 

	    	var operation = $(this).data('oper');
	    	if (operation === 'remove') {
				formObj.attr('action', '/answer/remove').submit();
	      	}
			else if (operation === "get") {
				var asno = $('input[name="asno"]').clone();
				$('#frmGoGet').submit();
			} 
			else if (operation === 'modify') {
				var str = '';
				$('.uploadResult ul li').each(function(i, obj) {
					var jobj = $(obj);
					str += '<input type="hidden" name="attachList['+i+'].fileName"   value="'+jobj.data("filename")+'">';
					str += '<input type="hidden" name="attachList['+i+'].uuid"       value="'+jobj.data("uuid"    )+'">';
					str += '<input type="hidden" name="attachList['+i+'].uploadPath" value="'+jobj.data("path"    )+'">';
					str += '<input type="hidden" name="attachList['+i+'].fileType"   value="'+jobj.data("type"    )+'">';
				});
				formObj.append(str).submit();
			}
		});
	});
</script>


<%@ include file="../includes/footer.jsp"%>
