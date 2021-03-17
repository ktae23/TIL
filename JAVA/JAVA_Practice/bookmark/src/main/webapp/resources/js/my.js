$(document).ready(function(){
	$("#memberInsertBtn").click(function(){
	
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
			    opener.parent.location.reload();
			    alert(data);
				window.close();
			  });
	});
});

$(document).ready(function(){
	$("#bookmarkInsertBtn").click(function(){
	
		var memid=$("#memid").val();
		var title=$("#name").val();
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
			    opener.parent.location.reload();
				window.close();
			  });
	});
});


$(document).ready(function(){
	$("#loginBtn").click(function(){//로그인 처리	
		
		var id=$("#id").val();
		var pw=$("#pw").val();
		
		
		$.post("../../login",
			  {			   
			    id:id,
			  },
			  function(data, status){	
			  		window.close();   
			  }
		);//end post() 
	});//end 로그인 처리
});


$(document).on("click", "#logoutBtn", function(event) {
	
		$.post("../../logout",
			  {			   
			   
			  },
			  function(data, status){		  	
			  	
			  	$.removeCookie("logined");	    
				location.reload();						   
			  }
		);
	});