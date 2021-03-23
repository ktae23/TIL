$(document).ready(function(){
	
	$("#orderBtn").click(function(){
		
	let confirm_data=confirm("다음과 같이 주문하시겠습니까?\n"+items);		
		if(confirm_data){
			alert("주문완료");
		}
		window.close();
	});

	$("#anotherBtn").click(function(){		
		window.close();
	});

	$("#cancelBtn").click(function(){
		alert("장바구니를 모두 비웁니다");		
		$.removeCookie("basket", { path: '/' });// 장바구니 쿠키 삭제
		window.close();
	});

	$(".orderForm").click(function(event){
		let choice_product_name=event.target.name;
		//alert(choice_product_name);
		let basket=$.cookie("basket");
		let obj=null;
		if(basket){ //쿠키에 장바구니가 있으면
			obj=JSON.parse(basket);//json을 javascript 객체로 바꿔서
			let flag=true;
			obj.product.forEach(function(item){ // 하나씩의 아이템(품목)을 꺼내서
				
				if(item.name==choice_product_name){//품목 이름이 클릭한 상품의 이름과 같으면
					item.quantity +=1; //상품의 수량을 1증가
					//alert("if:"+item.name+":"+item.quantity);
					flag=false;
				}
			});
			if(flag){//품목 이름이 클릭한 상품의 이름과 다르면
					obj.product.push({name:choice_product_name,quantity:1});//새 품목 추가
					//alert("else:"+obj.product[obj.product.length-1].name+":"+obj.product[obj.product.length-1].quantity);
			}
		}else{ //쿠키에 장바구니가 없으면 
			obj={
				product:[{name:event.target.name,quantity:1}] //첫 품목 추가
			};
		}
		basket=JSON.stringify(obj); //자바스크립트 객체를 json 형식으로 바꿔서
		$.cookie("basket",basket, { path: '/' });// 장바구니 쿠키 저장
		window.open('html/orderForm.html', '_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=600,height=350');
	});


	$("#memberInsertBtn").click(function(){//회원 가입 처리
	
		var name=$("#name").val();
		var id=$("#id").val();
		var pw=$("#pw").val();
		
		//alert(name+":"+id+":"+pw);
		
		$.post("../memberInsert",
			  {
			    name:name,
			    id:id,
			    pw:pw
			  },
			  function(data, status){
			    alert( data);
			    window.close();
			  });
		
	});



		$("#loginBtn").click(function(){//로그인 처리	
		
		var id=$("#id").val();
		var pw=$("#pw").val();
		
		//alert(id+":"+pw);		
		
		$.post("login",
			  {			   
			    id:id,
			    pw:pw
			  },
			  function(data, status){	
			  var obj=JSON.parse(data);			  
			  	if(obj.name){
			  		data = obj.name+"<input type='button' value='logout' id='logoutBtn' class='btn btn-primary'>";	
			  		$.cookie("logined",data);	    
					$("#msgDiv").html(data );		
				}else{
					alert(obj.msg);
					location.reload();	
				}				   
			  }
		);//end post() 
	});//end 로그인 처리

$(document).on("click", "#logoutBtn", function(event) { //로그아웃 처리
	
		$.post("logout",
			  {			   
			   
			  },
			  function(data, status){		  	
			  	
			  	$.removeCookie("logined");	    
				location.reload();						   
			  }
		);//end post() 
	});//end 로그아웃 처리
	
});