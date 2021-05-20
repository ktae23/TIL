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

          		<div class="form-group">
          			<label>번호</label> 
          			<input class="form-control" name="bno" value="<c:out value="${board.bno}"/>" readonly="readonly">
				</div>
        		<div class="form-group">
          			<label>제목</label> 
          			<input class="form-control" name="title" value="<c:out value="${board.title }"/>" readonly="readonly">
        		</div>
        		<div class="form-group">
          			<label>내용</label>
          			<textarea class="form-control" rows="3" name="content" readonly="readonly"><c:out value="${board.content}"/></textarea>
        		</div>
				<div class="form-group">
          			<label>작성자</label> 
          			<input class="form-control" name="writer" value="<c:out value="${board.writer }"/>" readonly="readonly">
        		</div>
				<div class="form-group">
				  	<label>등록일</label> 
				  	<input class="form-control" name="regDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${board.regDate}"/>" readonly="readonly">            
				</div>
				<div class="form-group">
				  	<label>수정일</label> 
				  	<input class="form-control" name="updateDate" value="<fmt:formatDate pattern="yyyy/MM/dd" value="${board.updateDate}"/>" readonly="readonly">            
				</div>

				<button data-oper="modify" class="btn btn-primary">수정</button>
				<button data-oper="list"   class="btn btn-info"   >목록으로</button>

				<form id="operForm" action="/admin/boadModify" method="get">
					<input type="hidden" name="bno"     value="<c:out value="${board.bno}"/>" id="bno">
					<input type="hidden" name="pageNum" value="<c:out value="${cri.pageNum}"/>">
					<input type="hidden" name="amount"  value="<c:out value="${cri.amount}"/>" >
					<input type="hidden" name="keyword" value="<c:out value="${cri.keyword}"/>">
					<input type="hidden" name="type"    value="<c:out value="${cri.type}"/>"   >  
				</form>
      		</div>
    	</div>
  	</div>
</div>

<%--
<div class="bigPictureWrapper">
	<div class="bigPicture"></div>
</div>
--%>
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

<%--
<style>
	.bigPictureWrapper {
  		background: rgba(255,255,255,0.5);
  	}
	.bigPicture img {
  		width: 600px;
	}
</style>
--%>
<script type="text/javascript">
	$(document).ready(function() {
		(function() {
			var bno = '<c:out value="${board.bno}"/>';
    	
			$.getJSON('/board/getAttachList', { bno : bno }, function(arr) {
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

<%-- 댓글 목록 --%>
<div class="row">
  	<div class="col-lg-12">
    	<div class="panel panel-default">
      		<div class="panel-heading">
        		<i class="fas fa-comments"></i> 댓글
        		<button id="addReplyBtn" class="btn btn-primary btn-xs pull-right">댓글달기</button>
      		</div>      
      		<div class="panel-body">        
      			<ul class="chat"></ul>
			</div>
			<div class="pull-right" id="panel-footer"></div>
		</div>
  	</div>
</div>

<%-- 댓글달기 모달 창 --%>
<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
				<h4 class="modal-title" id="myModalLabel">댓글달기</h4>
			</div>
			<div class="modal-body">
				<div class="form-group">
					<label>댓글</label> 
					<input class="form-control" name='reply' value='New Reply!!!!'>
				</div>      
				<div class="form-group">
					<label>작성자</label> 
					<input class="form-control" name='replyer' value='replyer'>
				</div>
				<%--
				<div class="form-group">
					<label>등록일</label> 
					<input class="form-control" name='replyDate' value='2018-01-01 13:13'>
				</div>
				--%>
			</div>
			<div class="modal-footer">
				<button type="button" id="modalModBtn"      class="btn btn-warning">Modify</button>
				<button type="button" id="modalRemoveBtn"   class="btn btn-danger" >Remove</button>
				<button type="button" id="modalRegisterBtn" class="btn btn-primary">등록</button>
				<button type="button" id="modalCloseBtn"    class="btn btn-default">취소</button>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript" src="/resources/js/reply.js"></script>
<script>
	$(document).ready(function () {
		var bnoValue = '<c:out value="${board.bno}"/>';
		var replyUL = $('.chat');

		showList(1);

		function showList(page) {
			replyService.getList({ bno : bnoValue, page : page || 1 }, function (replyCnt, list) {

				console.log('replyCnt = ' + replyCnt);
				console.log('list = ' + list);
				console.log(list);
				
				if (page == -1) {
					pageNum = Math.ceil(replyCnt / 10.0);
					showList(pageNum);
					return;
				}

				var str = "";
				if (list == null || list.length == 0) {
					return;
				}

				for (var i = 0, len = list.length || 0; i < len; i++) {
					str += '<li class="left clearfix" data-rno="' + list[i].rno + '">';
					str += '  	<div>';
					str += '		<div class="header">';
					str += '			<strong class="primary-font">[' + list[i].rno + '] ' + list[i].replyer + '</strong>';
					str += '			<small class="pull-right text-muted">' + replyService.displayTime(list[i].regDate) + '</small>';
					str += '		</div>';
					str += '		<p>' + list[i].reply + '</p>';
					str += '	</div>';
					str += '</li>';
				}
				replyUL.html(str);
				showReplyPage(replyCnt);
			});
		}

		<%-- 댓글 페이징 처리 --%>
		var pageNum = 1;
		var replyPageFooter = $("#panel-footer");
		function showReplyPage(replyCnt) {
			var endNum = Math.ceil(pageNum / 10.0) * 10;
			var startNum = endNum - 9;

			var prev = startNum != 1;
			var next = false;

			if (endNum * 10 >= replyCnt) {
				endNum = Math.ceil(replyCnt / 10.0);
			}

			if (endNum * 10 < replyCnt) {
				next = true;
			}

			var str = "<ul class='pagination pull-right'>";
			if (prev) {
				str += "<li class='page-item'><a class='page-link btn-xs' href='" + (startNum - 1) + "'>Previous</a></li>";
			}

			for (var i = startNum; i <= endNum; i++) {
				var active = pageNum == i ? "active" : "";
				str += "<li class='page-item "+active+" '><a class='page-link btn-xs' href='"+i+"'>" + i + "</a></li>";
			}

			if (next) {
				str += "<li class='page-item'><a class='page-link btn-xs' href='" + (endNum + 1) + "'>Next</a></li>";
			}
			str += "</ul></div>";

			replyPageFooter.html(str);
		}

		replyPageFooter.on("click", "li a", function(e) {
			e.preventDefault();

			var targetPageNum = $(this).attr("href");
			pageNum = targetPageNum;

			showList(pageNum);
		});


		var modal = $(".modal");
		var modalInputReply = modal.find("input[name='reply']");
		var modalInputReplyer = modal.find("input[name='replyer']");
		// var modalInputReplyDate = modal.find("input[name='replyDate']");

		var modalModBtn = $("#modalModBtn");
		var modalRemoveBtn = $("#modalRemoveBtn");
		var modalRegisterBtn = $("#modalRegisterBtn");

		$("#modalCloseBtn").on("click", function(e) {
			modal.modal('hide');
		});

		$("#addReplyBtn").on("click", function(e) {
			modal.find("input").val("");
			// modalInputReplyDate.closest("div").hide();
			modal.find("button[id !='modalCloseBtn']").hide();

			modalRegisterBtn.show();

			$(".modal").modal("show");
		});

		modalRegisterBtn.on("click", function(e) {
			var reply = {
				reply : modalInputReply.val(),
				replyer : modalInputReplyer.val(),
				bno : bnoValue
			};

			replyService.add(reply, function(result) {
				alert(result);

				modal.find("input").val("");
				modal.modal("hide");
				showList(1);
			});
		});

		$(".chat").on("click", "li", function(e) {
			var rno = $(this).data("rno");

			replyService.get(rno, function(reply) {
				modalInputReply.val(reply.reply);
				modalInputReplyer.val(reply.replyer);
				// modalInputReplyDate.val(replyService.displayTime(reply.replyDate)).attr("readonly", "readonly");
				
				modal.data("rno", reply.rno);
				modal.find("button[id !='modalCloseBtn']").hide();
				modalModBtn.show();
				modalRemoveBtn.show();

				$(".modal").modal("show");
			});
		});

		modalModBtn.on("click", function(e) {
			var reply = {
				rno : modal.data("rno"),
				reply : modalInputReply.val()
			};

			replyService.update(reply, function(result) {
				alert(result);
				modal.modal("hide");
				showList(pageNum);
			});

		});

		modalRemoveBtn.on("click", function(e) {
			var rno = modal.data("rno");
			replyService.remove(rno, function(result) {
				alert(result);
				modal.modal("hide");
				showList(pageNum);
			});
		});
	});
</script>

<script type="text/javascript">
	$(document).ready(function() {
		var operForm = $("#operForm");

		$("button[data-oper='modify']").on("click", function(e) {
			operForm.attr("action", "/admin/boardModify").submit();
		});

		$("button[data-oper='list']").on("click", function(e) {
			operForm.find("#bno").remove();
			operForm.attr("action", "/admin/boardList")
			operForm.submit();
		});
	});
</script>


<%@include file="../includes/footer.jsp"%>
