
// 로그인, 로그아웃

$(document).ready(function(){
	$("#loginBtn").click(function(){ // 로그인
		
		var id=$("#id").val();
		var pw=$("#pw").val();
		
		
		$.post("../../memberlogin",
			  {			   
			    id:id,
			    pw:pw
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
		  		window.close();   
			  }
		);
	});
});



$(document).on("click", "#logoutBtn", function(event) { // 로그아웃
	
		$.post("../../logout",
			  {			   
			   
			  },
			  function(data, status){		  	
			  	
			  	$.removeCookie("logined");	    
				location.reload();						   
			  }
		);
	});

// 멤버 CRUD

$(document).ready(function(){
	$("#memberInsertBtn").click(function(){ // 멤버 추가
	
		var name=$("#name").val();
		var id=$("#id").val();
		var pw=$("#pw").val();
			
		$.post("../../memberInsert",
			  {
			    name:name,
			    id:id,
			    pw:pw
			  },
			  function(data, status){
			    alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});


$(document).ready(function(){
	$("#memberUpdateBtn").click(function(){ // 멤버 수정
	
		var name=$("#name").val();
		var id=$("#id").val();
		var pw=$("#pw").val();

		
			
		$.post("../../memberUpdate",
			  {
			    name:name,
			    id:id,
			    pw:pw
			   
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});




$(document).ready(function(){
	$("#memberDelelteBtn").click(function(){ // 멤버 삭제
	

		var id=$("#id").val();
		var pw=$("#pw").val();

		
			
		$.post("../../memberDelete",
			  {

			    id:id,
				pw:pw
			   
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});




// 북마크 CRUD


$(document).ready(function(){
	$("#bookmarkInsertBtn").click(function(){ // 북마크 추가
	
		var memid=$("#memid").val();
		var title=$("#title").val();
		var url=$("#url").val();
		var coment=$("#coment").val();
		
			
		$.post("../../bookmarkInsert",
			  {
			  	memid:memid,
			    title:title,
			    url:url,
			    coment:coment
			   
			  },
			  function(data, status){
			    alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});

$(document).ready(function(){
	$("#bookmarkUpdateBtn").click(function(){ // 북마크 수정
	
		var bookmark_no=$("#bookmark_no").val();
		var memid=$("#memid").val();
		var title=$("#title").val();
		var url=$("#url").val();
		var coment=$("#coment").val();

		
			
		$.post("../../bookmarkUpdate",
			  {
			  	bookmark_no:bookmark_no,
			  	memid:memid,
			    title:title,
			    url:url,
			    coment:coment
			   
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});




$(document).ready(function(){
	$("#bookmarkDeleteBtn").click(function(){ // 북마크 삭제
	
		var bookmark_no=$("#bookmark_no").val();
		var memid=$("#memid").val();
		
			
		$.post("../../bookmarkDelete",
			  {
			  	memid:memid,
			  	bookmark_no:bookmark_no,
			  	
			   
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});




