<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
  	<div class="col-lg-12">
    	<h3 class="page-header">공지사항</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-bullhorn"></i>
      		</div>
    		<div class="panel-body">
				<form role="form" action="/board/modify" method="post">
					<input type="hidden" name="pageNum" value="<c:out value="${cri.pageNum}"/>">
					<input type="hidden" name="amount"  value="<c:out value="${cri.amount }"/>">
					<input type="hidden" name="type"    value="<c:out value="${cri.type   }"/>">
					<input type="hidden" name="keyword" value="<c:out value="${cri.keyword}"/>">
				 
					<div class="form-group">
					  	<label>번호</label> 
					  	<input class="form-control" name="bno" value="<c:out value="${board.bno}"/>" readonly="readonly">
					</div>
					<div class="form-group">
					  	<label>제목</label> 
					  	<input class="form-control" name="title" value="<c:out value="${board.title}"/>" >
					</div>
					<div class="form-group">
					  	<label>내용</label>
					  	<textarea class="form-control" rows="3" name="content" ><c:out value="${board.content}"/></textarea>
					</div>
					<div class="form-group">
					  	<label>작성자</label> 
					  	<input class="form-control" name="writer" value="<c:out value="${board.writer}"/>" readonly="readonly">            
					</div>
				  	<button type="submit" data-oper="modify" class="btn btn-primary">수정</button>
				  	<button type="submit" data-oper="remove" class="btn btn-danger" >삭제</button>
				  	<button type="submit" data-oper="list"   class="btn btn-info"   >목록으로</button>
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

<style>
	.bigPictureWrapper {
  		background: rgba(255,255,255,0.5);
  	}
	.bigPicture img {
  		width: 600px;
	}
</style>
<script type="text/javascript">

	$(document).ready(function() {
		(function() {
			var bno = '<c:out value="${board.bno}"/>';
			$.getJSON('/board/getAttachList', { bno : bno }, function(arr) {
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

		var regex = new RegExp('(.*?)\.(exe|sh|zip|alz)$');
		var maxSize = 5242880; // 5MB

		function checkExtension(fileName, fileSize) {
			if (fileSize >= maxSize) {
				alert("파일 사이즈 초과");
				return false;
			}
			if (regex.test(fileName)) {
				alert("해당 파일 종류는 업로드할 수 없습니다.");
				return false;
			}
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
		var formObj = $("form");
		$('button').on('click', function(e) {
	    	e.preventDefault(); 
	    
	    	var operation = $(this).data('oper');
	    	if (operation === 'remove') {
				formObj.attr('action', '/board/remove');
	      	}
			else if (operation === "list") {
				var pageNum = $('input[name="pageNum"]').clone();
				var amount  = $('input[name="amount" ]').clone();
				var keyword = $('input[name="keyword"]').clone();
				var type    = $('input[name="type"   ]').clone();      
 
				<%-- 목록으로 이동에 필요한 파라미터를 제외한 나머지는 모두 삭제 --%>
	      		formObj.empty();
	      		formObj.attr('action', '/board/list').attr('method', 'get');
  				formObj.append(pageNum);
				formObj.append(amount );
				formObj.append(keyword);
				formObj.append(type   );	       
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
	        formObj.submit();
		});
	});
</script>


<%@ include file="../includes/footer.jsp"%>
