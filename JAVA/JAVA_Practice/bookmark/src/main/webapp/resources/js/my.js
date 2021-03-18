
// 로그인

$(document).ready(function(){
	$("#loginBtn").click(function(){ // 로그인
		
		var id=$("#id").val();
		var pw=$("#pw").val();
		
		if(id == 'admin' && pw =='0000'){
			$.post("../../memberlogin",
				  {			   
				    id:id,
				    pw:pw
				  },
				  function(data, status){
				    var obj=JSON.parse(data);			  
			  	if(obj.name){
				 	alert(obj.name + "님 환영합니다.");
					opener.parent.$.cookie("logined4admin",obj.name + "님 환영합니다.");	
					opener.parent.location.reload();
					window.close();
				}else{
					alert(obj.msg);
					location.reload();	
				}	
			});
		}else{ 
			$.post("../../memberlogin",
				  {			   
				    id:id,
				    pw:pw
				  },
				  function(data, status){
				  var obj=JSON.parse(data);			  
			  	if(obj.name){
				 	 alert(obj.name+ "님 환영합니다.");
					opener.parent.$.cookie("logined",obj.name + "님 환영합니다.");	
					opener.parent.location.reload();	
					window.close();
				  }else{
					alert(obj.msg);
					location.reload();	
				}	
			});
		}
	});
});

// 로그아웃

$(document).on("click", "#logoutBtn", function(event) { 
		$.post("../../logout",
			  {			   
			   
			  },
			  function(data, status){		  	
			  	
			  	opener.parent.$.removeCookie("logined");
				opener.parent.$.removeCookie("logined4admin");
				opener.parent.location.href='../../';	
				window.close();					   
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
			  	opener.parent.$.removeCookie("logined");
				opener.parent.$.removeCookie("logined4admin");
				opener.parent.location.href='../../';	
				window.close();					   
			  });
	});
});



// 북마크 CRUD


$(document).ready(function(){
	$("#bookmarkInsertBtn").click(function(){ // 북마크 추가
	
		var title=$("#title").val();
		var url=$("#url").val();
		var coment=$("#coment").val();
		
			
		$.post("../../bookmarkInsert",
			  {
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
		var pw=$("#pw").val();
		
			
		$.post("../../bookmarkUpdate",
			  {
			  	bookmark_no:bookmark_no,
			  	memid:memid,
			    title:title,
			    url:url,
			    coment:coment,
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
	$("#bookmarkDeleteBtn").click(function(){ // 북마크 삭제
	
		var bookmark_no=$("#bookmark_no").val();
		var pw=$("#pw").val();		
			
		$.post("../../bookmarkDelete",
			  {
			  	bookmark_no:bookmark_no,
				pw:pw
			  	
			   
			  },
			  function(data, status){
			  	alert(data);
			    opener.parent.location.reload();
				window.close();
			  });
	});
});/**
 * 
 */