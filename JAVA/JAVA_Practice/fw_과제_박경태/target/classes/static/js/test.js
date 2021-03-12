$(document).ready(function(){
	$('#login_btn').click(function(){
		var id = $("#id").val();
		$.ajax({
			type:"post",
			async:false,
//			url:"http://localhost:9999/ai_fw_test/login",
			url:"./login",
			data:id,
			success:function(data,status){
				alert("Data: " + id + "님 login ok \nStatus: " + status);
			},
		})
	})
})

