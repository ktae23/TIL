### 채팅창 구현

#### chat_section.html

```html
<div id="chat_section" th:fragment="chat_section">
    <div class="chatbody">
        <div class="panel panel-primary">

            <div class="panel-heading top-bar">
                <div class="col-md-8 col-xs-8">
                    <h3 class="panel-title"><span class="glyphicon glyphicon-comment"></span> Movie Mentor</h3>
                </div>
            </div>
            <div class="col-sm-3 col-sm-offset-4 frame">
                <ul class="message-box"></ul>
                <div>
                    <div class="msj-rta macro" style="margin:auto">                        
                        <div class="text text-r" style="background:whitesmoke !important">
                            <input class="mytext" placeholder="Type a message"/>
                        </div> 
                        &nbsp;&nbsp;&nbsp;<button class="btn btn-primary-dark btn-lg" id="btn-chat"><i class="fa fa-send fa-1x" aria-hidden="true"></i></button>
                    </div>
                </div>
            </div>    
        </div>
    </div>

</div>
```

<br/>

#### chat.js

```javascript
//검색

$(document).ready(
		function() {
			var me = {};

			var you = {};

			function formatAMPM(date) {
				var hours = date.getHours();
				var minutes = date.getMinutes();
				var ampm = hours >= 12 ? 'PM' : 'AM';
				hours = hours % 12;
				hours = hours ? hours : 12; // the hour '0' should be '12'
				minutes = minutes < 10 ? '0' + minutes : minutes;
				var strTime = hours + ':' + minutes + ' ' + ampm;
				return strTime;
			}

			// 채팅 기능을 구현한 코드
			function insertChat(who, text, time) {
                // html이 들어갈 변수
				var control = "";
				var date = formatAMPM(new Date());

				if (who == "me") {

					control = '<li style="width:70%">'
							+ '<div class="msj macro" style="margin-left:135px! important; margin-bottom:10px! important; margin-top:10px! important;">'
							+ '<div class="text text-r">' + '<br/>' + '<p>'
							+ text + '</p>' + '<p style="text-align:left;"><small>' + date
							+ '</small></p>' + '<br/>' + '</div>' + '</div>'
							+ '</li>';
				} else {
					control = '<li style="width:70%;">'
							+ '<div class="msj-rta macro">'
							+ '<div class="text text-l">' + '<br/>' + '<p>'
							+ text + '</p>' + '<p><small>' + date
							+ '</small></p>' + '<br/>' + '</div>' + '</div>'
							+ '</li>';
				}
				setTimeout(function() {
					$(".message-box").append(control).scrollTop($(".message-box").prop('scrollHeight'));

				}, time);
			}
			
			// 아래 코드는 SPA(Single Page Application) 구현을 위해 movie_section에 영화 정보를 실시간으로 넣어주기 위한 코드입니다.
            // https://ktae23.tistory.com/155
            // https://github.com/moviementorteam/MM
            
			function insertMovie(movies, posters, time) {
				var control = "";
				for(var i=0; i<movies.length; i++){
					control += '<span>'
							+'<div class="col-lg-4 col-md-6 mb-4">'
		            		+'<div align="center" class="card h-100">'
		            		+'<div class="card-header text-white bg-secondary">'
		            		+'<h4 class="movieTitle">'+movies[i].movieTitle+'</h4></div>'
		            		+'<div class="card-body">'
		            		+'<p> 장르 : ' + movies[i].movieGenre + '</p>'
		            		+'<p> 평점 : ' + movies[i].movieRating +'</p>'
		            		+'<span>'
		                  	+'<img id="poster" alt="'+movies[i].movieTitle+ ' 포스터" src="/poster/'+ posters[i].posterPath + '/'+'"  width="25" height="25%"/>'
		                  	+'<div class = "image_overlay image_overlay_blur">'
							+'<div class = "image_movieStory" >'+movies[i].movieStory+'</div>'
							+'<br><br><br>'
				            +'<div> <button class="movieDetailBtn" id="'+i+'" name="movieDetailBtn">자세히 보기</button></div>'
				            +'<br>'
					        +'<div> <button class="cartInsertBtn" id="'+i+'" name="cartInsertBtn">담기</button></div>'
					        +'<br>'
							+'</div>'
							+'</span>'
							+'<br><br>'
							+'<p>'+new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(movies[i].moviePrice)+'</p>'
			                +'<br>'
			                +'</div>'
			                +'</div>'
			                +'</div>'
			                +'</span>';
				}
				
                
				setTimeout(function() {
					if(control != ""){
						$(".movie-box").html(control);
					}

				}, time);

			}
			
            // 채팅창을 비움
			function resetChat() {
				$(".message-box").empty();
			}
			
            // 텍트스 창이 활성화 되었을 때
			$(".mytext").on("keyup", function(e) {
                // 엔터키가 눌렸을 다면
				if (e.which == 13) {
					var text = $(this).val();
					if (text !== "") {
                        // 채팅을 입력하고 빈칸으로 되돌림
						insertChat("me", text);
						$(this).val('');
					}
					// 챗봇 api로 값을 보낸 뒤 받아온 JSON 값을 꺼내주는 코드
					$.get("chat", {
						chat : text
					}, function(data) {
						var obj=JSON.parse(data);
						var text = obj.chatMsg;
						var movies = obj.movies;
						var posters = obj.posters;
						
						if (text !== "") {
                            // 챗봇의 답변을 입력
							insertChat("mm", text, 15);
                            // 결과에 해당하는 영화를 입력
							insertMovie(movies, posters, 15);
						}
					});
				}
			});

			resetChat();
            //welcome message
			insertChat("mm", "안녕하세요. 취향에 맞는 영화를 찾아주는 MM입니다.<br><br>'영화 추천', '취향에 맞는 영화' 등을 입력해서 취향에 맞는 영화를 찾아보세요.<br><br>원하는 영화 결과가 없거나 처음부터 다시 시작하고 싶으신 경우 채팅창에 '다시'를 입력 해주세요.", 0);
		});


<br/>

[채팅 창 부트스트랩 예제](https://bootsnipp.com/tags/chat)

위 사이트 중 하나에서 HTML, CSS, JS를 가져와 수정하여 사용함