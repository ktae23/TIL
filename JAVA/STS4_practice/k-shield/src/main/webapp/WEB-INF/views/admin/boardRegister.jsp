<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
  	<div class="col-lg-12">
    	<h3 class="page-header">공지관리</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-th-list"></i>
      		</div>
			<div class="panel-body">
       			<form role="form" action="/admin/boardRegister" method="post">
         			<div class="form-group">
           				<label>제목</label> 
           				<input class="form-control" name="title">
         			</div>
         			<div class="form-group">
           				<label>내용</label>
           				<textarea class="form-control" rows="3" name="content"></textarea>
         			</div>
					<div class="form-group">
           				<label>작성자</label> <input class="form-control" name="writer">
         			</div>
 
         			<button type="submit" data-oper="save"   class="btn btn-primary">저장</button>
         			<button type="submit" data-oper="cancel" class="btn btn-info"   >취소</button>
         		</form>
			</div>
		</div>
	</div>
</div>

<%-- 첨부파일 --%>
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
	$(document).ready(function(e){
	    var formObj = $('form[role="form"]');
	    $('button[type="submit"]').on('click', function(e) {
	        e.preventDefault();
	
	        var s = '';
	        $('.uploadResult ul li').each(function(i, obj) {
	            var o = $(obj);
	            s += '<input type="hidden" name="attachList['+i+'].fileName"   value="'+o.data('filename')+'">';
	            s += '<input type="hidden" name="attachList['+i+'].uuid"       value="'+o.data('uuid')    +'">';
	            s += '<input type="hidden" name="attachList['+i+'].uploadPath" value="'+o.data('path')    +'">';
	            s += '<input type="hidden" name="attachList['+i+'].fileType"   value="'+o.data('type')    +'">';
	        });
	        formObj.append(s).submit();
	    });
	
	    var regex = new RegExp('(.*?)\.(exe|sh|zip|alz)$');
	    var maxSize = 5242880; // 5MB
	
	    function checkExtension(fileName, fileSize) {
	        if (fileSize >= maxSize) {
	            alert('파일 사이즈 초과');
	            return false;
	        }
	        if (regex.test(fileName)) {
	            alert('해당 파일 종류는 업로드할 수 없습니다.');
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
	            contentType: false,data: 
	            formData,type: 'POST',
	            dataType:'json',
	            success: function(result) {
	                console.log(result); 
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
	
	    <%-- 첨부 파일 변경 --%>
	    $('.uploadResult').on('click', 'button', function(e) {
	        var targetFile = $(this).data('file');
	        var type = $(this).data('type');
	        var targetLi = $(this).closest('li');
	        $.ajax({
	            url: '/deleteFile',
	            data: { fileName: targetFile, type:type },
	            dataType: 'text',
	            type: 'POST',
	            success: function(result) {
	                alert(result);
	                targetLi.remove();
	            }
	        }); //$.ajax
	    });
	});
</script>


<%@include file="../includes/footer.jsp"%>
