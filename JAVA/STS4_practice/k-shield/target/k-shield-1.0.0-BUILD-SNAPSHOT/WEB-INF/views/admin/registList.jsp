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
				<table class="table table-striped table-bordered table-hover">
				<thead>
				<tr>
					<th>번호</th>
					<th>이름</th>
					<th>이메일</th>
					<th>연락처</th>
					<th>등록일</th>
					<th>수정일</th>
				</tr>
				</thead>

				<c:forEach items="${list}" var="rgvo">
				<tr>
					<td><c:out value="${rgvo.rgno}"/></td>
					<td>
						<a class="move" href="<c:out value="${rgvo.rgno}"/>">
							<c:out value="${rgvo.name}"/>
						</a>
					</td>
					<td><c:out value="${rgvo.email}"/></td>
					<td><c:out value="${rgvo.phone}"/></td>
					<td><fmt:formatDate pattern="yyyy-MM-dd" value="${rgvo.regDate}"/></td>
					<td><fmt:formatDate pattern="yyyy-MM-dd" value="${rgvo.updateDate}"/></td>
				</tr>
				</c:forEach>
				</table>

				<div class="row">
					<div class="col-lg-12">
						<form class="form-inline" id="searchForm" action="/admin/registList" method="get">
							<div class="form-group">
								<select name="type" class="form-control">
									<option value=""    <c:out value="${pageMaker.cri.type == null  ? 'selected':''}"/>>--</option>
									<option value="N"   <c:out value="${pageMaker.cri.type eq 'N'   ? 'selected':''}"/>>이름</option>
									<option value="E"   <c:out value="${pageMaker.cri.type eq 'E'   ? 'selected':''}"/>>이메일</option>
									<option value="P"   <c:out value="${pageMaker.cri.type eq 'P'   ? 'selected':''}"/>>연락처</option>
									<option value="NE"  <c:out value="${pageMaker.cri.type eq 'NE'  ? 'selected':''}"/>>이름+이메일</option>
									<option value="NP"  <c:out value="${pageMaker.cri.type eq 'NP'  ? 'selected':''}"/>>이름+연락처</option>
									<option value="NEP" <c:out value="${pageMaker.cri.type eq 'NEP' ? 'selected':''}"/>>이름+이메일+연락처</option>
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

			<form id="actionForm" action="/admin/registList" method="get">
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

		var actionForm = $("#actionForm");
		$(".paginate_button a").on("click", function(e) {
			e.preventDefault();
			actionForm.find("input[name='pageNum']").val($(this).attr("href"));
			actionForm.submit();
		});

		$(".move").on("click", function(e) {
			e.preventDefault();
			actionForm.append("<input type='hidden' name='rgno' value='" + $(this).attr("href") + "'>");
			actionForm.attr("action", "/admin/registGet");
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
