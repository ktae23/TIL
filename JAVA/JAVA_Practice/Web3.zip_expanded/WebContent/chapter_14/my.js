$(document).ready(function(){
	$('header li').hover(function(){
		$(this).addClass('active');
	},function(){
		$(this).removeClass('active');
	});
});