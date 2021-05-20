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
				<button id="regBtn" type="button" class="btn btn-xs pull-right btn-primary">공지사항 등록</button>
			</div>
			<div class="panel-body">
				<table class="table table-striped table-bordered table-hover">
				<thead>
				<tr>
					<th>번호</th>
					<th>제목</th>
					<th>작성자</th>
					<th>등록일</th>
					<th>수정일</th>
				</tr>
				</thead>

				<c:forEach items="${list}" var="board">
				<tr>
					<td><c:out value="${board.bno}"/></td>
					<td>
						<a class="move" href="<c:out value="${board.bno}"/>">
							<c:out value="${board.title}"/>
							<b>[<c:out value="${board.replyCnt}"/>]</b>
						</a>
					</td>
					<td><c:out value="${board.writer}"/></td>
					<td><fmt:formatDate pattern="yyyy-MM-dd" value="${board.regDate}"/></td>
					<td><fmt:formatDate pattern="yyyy-MM-dd" value="${board.updateDate}"/></td>
				</tr>
				</c:forEach>
				</table>

				<div class="row">
					<div class="col-lg-12">
						<form class="form-inline" id="searchForm" action="/admin/boardList" method="get">
							<div class="form-group">
								<select name="type" class="form-control">
									<option value=""    <c:out value="${pageMaker.cri.type == null  ? 'selected':''}"/>>--</option>
									<option value="T"   <c:out value="${pageMaker.cri.type eq 'T'   ? 'selected':''}"/>>제목</option>
									<option value="C"   <c:out value="${pageMaker.cri.type eq 'C'   ? 'selected':''}"/>>내용</option>
									<option value="W"   <c:out value="${pageMaker.cri.type eq 'W'   ? 'selected':''}"/>>작성자</option>
									<option value="TC"  <c:out value="${pageMaker.cri.type eq 'TC'  ? 'selected':''}"/>>제목+내용</option>
									<option value="TWC" <c:out value="${pageMaker.cri.type eq 'TWC' ? 'selected':''}"/>>제목+내용+작성자</option>
								</select>
							</div>
							<div class="form-group">
								<input type="text"   name="keyword" value="<c:out value="${pageMaker.cri.keyword}"/>" class="form-control"/>
								<input type="hidden" name="pageNum" value="<c:out value="${pageMaker.cri.pageNum}"/>"/> 
								<input type="hidden" name="amount"  value="<c:out value="${pageMaker.cri.amount }"/>"/>
							</div> 
							<button class="btn btn-default">검색</button>
						</form>
					</div>
				</div>

				<div class="pull-right">
					<ul class="pagination">
						<c:if test="${pageMaker.prev}">
							<li class="paginate_button previous"><a href="${pageMaker.startPage - 1}">이전</a></li>
						</c:if>

						<c:forEach var="num" begin="${pageMaker.startPage}" end="${pageMaker.endPage}">
							<li class="paginate_button  ${pageMaker.cri.pageNum == num ? "active" : ""}">
								<a href="${num}">${num}</a>
							</li>
						</c:forEach>

						<c:if test="${pageMaker.next}">
							<li class="paginate_button next"><a href="${pageMaker.endPage + 1}">다음</a></li>
						</c:if>
					</ul>
				</div>
			</div>

			<form id="actionForm" action="/admin/boardList" method="get">
				<input type="hidden" name="pageNum" value="<c:out value="${pageMaker.cri.pageNum}"/>">
				<input type="hidden" name="amount"  value="<c:out value="${pageMaker.cri.amount }"/>">
				<input type="hidden" name="type"    value="<c:out value="${pageMaker.cri.type   }"/>"> 
				<input type="hidden" name="keyword" value="<c:out value="${pageMaker.cri.keyword}"/>">
			</form>
		</div>
	</div>
</div>


<script type="text/javascript">
	$(document).ready(function() {
		var result = '<c:out value="${result}"/>';
		history.replaceState({}, null, null);

		$("#regBtn").on("click", function() {
			self.location = "/admin/boardRegister";
		});

		var actionForm = $("#actionForm");
		$(".paginate_button a").on("click", function(e) {
			e.preventDefault();
			actionForm.find("input[name='pageNum']").val($(this).attr("href"));
			actionForm.submit();
		});

		$(".move").on("click", function(e) {
			e.preventDefault();
			actionForm.append("<input type='hidden' name='bno' value='" + $(this).attr("href") + "'>");
			actionForm.attr("action", "/admin/boardGet");
			actionForm.submit();
		});

		var searchForm = $("#searchForm");
		$("#searchForm button").on("click", function(e) {
			if (!searchForm.find("option:selected").val()) {
				alert("검색종류를 선택하세요");
				return false;
			}
			if (!searchForm.find("input[name='keyword']").val()) {
				alert("키워드를 입력하세요");
				return false;
			}
			searchForm.find("input[name='pageNum']").val("1");
			
			e.preventDefault();

			searchForm.submit();				
		});
	});
</script>






<%@include file="../includes/footer.jsp"%>
