### Thymeleaf Each문 이용한 반복 문에서 Index 설정하여 정보 가져오기

<br/>

#### 예시 코드 - 영화 리스트 출력

```html
<div id="movie_section" th:fragment="movie_section">
    			<!-- member는 model에서 가져오는 값-->
    			<div align="center" id="movie_main_title" th:text="${member.name} + '님을 위한 전체 영화'"></div>
    			<span class="movie-box">
                <!-- 아래 span 태그 안쪽은 model에서 가져 온 movies 내의 각 값들 만큼 생성 됨 -->
				<span th:each="movie : ${movies}">	
               			
			          <div class="col-lg-4 col-md-6 mb-4">
			            <div align="center" class="card h-100">
			            <div class="card-header text-white bg-secondary">
                            <h4 th:text="${movie.movieTitle}" class="movieTitle"></h4></div>
			              <div class="card-body">
			               
			                <p th:text="'장르 : ' + ${movie.movieGenre}"></p>
			                <p th:text="'평점 : ' + ${movie.movieRating}+'점'"></p>

			                <!-- 아래 span 태그 안쪽은 model에서 가져 온 posters 내의 각 값들 만큼 생성 되어야 하나
								상위 Each문의 movie와 같은 poster만 생성 되도록 조건문 작성 -->
			                <span th:if="${movie.movieCode == poster.movieCode}" th:each="poster : ${posters}">
			                  	<img  id="poster" th:alt="${movie.movieTitle}+' 포스터'" th:src="'/poster/' + ${poster.posterPath} + '/'"  width="25" height="25%"/>
			                  	<div  class = "image_overlay image_overlay_blur">
									<div class = "image_movieStory" th:text="${movie.movieStory}"></div>
									<br><br><br>
                                    	<!-- id 값으로 moive 객체가 moives에서 몇 번째 index 값인지 가져와서 입력 
												Javascript에서 이 값을 이용하여 반복 되는 값에서 원하는 값을 찾아 올 수 있음-->
						                <button class="movieDetailBtn" th:attrappend="id=${movieStat.index}" name="movieAllBtn">자세히 보기</button> 
					                	<br>
						           <div> <button class="cartInsertBtn" th:attrappend="id=${movieStat.index}" name="cartInsertBtn" >담기</button></div>
						            	<br>
								</div>
			                </span>
			                <br><br>
			                <p th:text="${#numbers.formatCurrency(movie.moviePrice)}"></p>
					                
				                <br>
			              </div>
		            	</div>
		            </div>
		            
				</span>
				</span>
			</div>
```

<br/>

#### Javascript

```javascript
//영화 자세히 보기
$(document).ready(function(){
    // name이 movieAllBtn인 button을 클릭 했을 때
	$("button[name=movieAllBtn]").click(function(){ 
        // 클릭 된 객체의 id 속성을 가져와 index란 이름의 변수에 넣어주고
		var index = $(this).attr("id");
        // .eq() 선택자 메서드를 이용하여 class가 movieTitle인 모든 요소들 중
        // index번째 요소의 text를 가져와 movieTitle이란 변수에 넣어 준다.
		var movieTitle=$('.movieTitle').eq(index).text();
		$.get('movieAllDetail',
				{			   
					movieTitle:movieTitle
				},
				function(data){
					window.open('movieAllDetail?movieTitle='+movieTitle,'_blank', 'toolbar=yes,scrollbars=yes,resizable=yes,top=50,left=500,width=500,height=500');
				});
		
	});
});
```



