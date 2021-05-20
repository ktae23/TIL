<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
  	<div class="col-lg-12">
    	<h3 class="page-header">참가신청</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-edit"></i>
      		</div>
			<div class="panel-body">
       			<form role="form" action="/regist/register" method="post" data-toggle="validator">
       			
       				
       				
         			<div class="form-group">
           				<label for="name" class="control-label">이름</label> 
           				<input class="form-control" name="name" id="name" placeholder="이름" required data-error="이름을 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>
         			<div class="form-group">
           				<label for="email" class="control-label">이메일</label>
           				<input class="form-control" name="email" id="email" placeholder="이메일" type="email" required data-error="이메일을 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>
         			<div class="form-group">
           				<label for="phone" class="control-label">연락처</label>
           				<input class="form-control" name="phone" id="phone" placeholder="연락처" type="tel" required data-error="연락처를 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>
 					<div class="form-group">
           				<label for="password" class="control-label">패스워드</label>
           				<input class="form-control" name="password" id="password" placeholder="패스워드" type="password" required data-error="패스워드를 입력하세요.">
           				<div class="help-block with-errors"></div>
         			</div>
         			<div class="form-group">
           				<label for="content" class="control-label">하고싶은 말</label>
           				<textarea class="form-control" rows="3" name="content" id="content" placeholder="하고싶은 말"></textarea>
         			</div>
 
         			<button type="submit" data-oper="save"   class="btn btn-primary">저장</button>
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
	
	    <%-- LAB File Upload : Insecure Code --%>
	    $('input[type="file"]').change(function(e) {
	    	var formData = new FormData();
	        var inputFile = $('input[name="uploadFile"]');
	        var files = inputFile[0].files;
	        for (var i = 0; i < files.length; i ++) {
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
	    <%-- /////////////////////////////// --%>
	    
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
	        });
	    });
	});
</script>


<%@include file="../includes/footer.jsp"%>
