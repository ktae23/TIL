$(document).ready(function(){
	$('#css_basic').click(function(){
		$('#main-section').hide();
		$('#css_basicDiv').html(
'<article><div class="article-header"><h1 class="article-title">CSS3 개요</h1><p class="article-date">2013년 02월 14일</p></div>');
		$('#css_basicDiv').show();
	});
	
	$('#html5').click(function(){
		$('#main-section').show();
		$('#css_basicDiv').hide();
	});
});