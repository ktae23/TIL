$(document).ready(function(){
	$('#main-header li').hover(function(){
		$(this).addClass('active');
	},function(){
		$(this).removeClass('active');
	});
	
	
	$('#network').click(function(){
		$('#main-content').hide();
		$('#sub-content').hide();
		$('#networkDiv').html('<h1>원하는 내용을 넣으세요</h1>');
		$('#networkDiv').show();
	});
	
	$('#store').click(function(){
		$('#main-content').show();
		$('#sub-content').show();
		$('#networkDiv').hide();
	});
});