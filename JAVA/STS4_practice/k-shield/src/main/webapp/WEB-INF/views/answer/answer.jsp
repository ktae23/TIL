<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ include file="../includes/header.jsp"%>


<div class="row">
  	<div class="col-lg-12">
    	<h3 class="page-header">정답제출</h3>
  	</div>
</div>

<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
      			<i class="fas fa-edit"></i>
      		</div>
			<div class="panel-body">
       			<form role="form" action="/answer/answer" method="post" data-toggle="validator">
       			
       				<%-- TODO CSRF Secure Code --%>
       				
       				
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
	        });
	    });
	});
</script>


<%@include file="../includes/footer.jsp"%>
