## jQuery 라이브러리 설정

- 모든 웹 브라우저에서 동작하는 클라이언트용 자바스크립트 라이브러리
- 애플리케이션 수준에서 사용하기엔 어렵지만 일반적인 웹페이지 개발시엔 충분 함
  - 쉬운 문서 객체 모델과 관련 처리 구현
  - 쉽고 일관된 이벤트 연결 구현
  - 쉬운 시각적 효과 구현
  - 쉬운 Ajax 응용 프로그램 개발

<br />

#### 라이브러리 구하는 방법

1. [jQuery ](http://jquery.com)제이 쿼리 사이트에서 다운로드 받아 외부 자바스크립트 파일로 추가
2. [CDN (Content Delivery Networ)](https://code.jquery.com/) 이용하여 코드 내에 URL 입력하여 사용

<br />

```html
<!DOCTYPE html>
<html>
    <head>
        <title>jQuery Basic</title>
        <script src = "https://code.jquery.com/jquery-3.4.1.js"></script>
        <script>
        	$(document).ready(funtion(){
                              <!--window.onload 이벤트와 같은 기능 수행-->
                              })
        </script>
    </head>
    <body>
        
    </body>
</html>
```

<br />

## 문서 객체 선택

- 자바스크립트는 식별자로 `$`와 `_`를 사용

```javascript
// $() 함수는 별도 기능이 있는 것이 아님
$(선택자).메서드(매개변수, 매개변수)

// jQuery()는 기본 함수로 자주 사용하여 변수를 대치 함
window.jquery = window.$ = jQuery;
```

<br />

```html
<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        <!--이벤트 연결-->
        $(document).ready(function () {
            <!--스타일 속성 변경-->
            $('h1').css('color', 'red');
            $('h1').css('background', 'black');
        });
    </script>
</head>
<body>
    <h1>Header</h1>
    <h1>Header</h1>
    <h1>Header</h1>
</body>
</html>
```

<br />

## 문서 객체 조작

### 속성 조작

- 문서 객체 속성을 조작할 때는 attr() 메서드 사용
- attr()메서드는 매개 변수를 넣는 방법에 따라 속성을 지정하거나 추출 가능

<br />

##### 속성 추출

```javascript
var src = $('img').attr('src');
// img 태그의 src 속성을 추출
```

- 위 코드는 atrr()메서드에 들어 온 매개변수의 속성을 추출한다
- jQuery는 여러 문서 객체를 한 번에 선택하므로 여러 개의 문서 객체 선택 후 속성 추출 시 맨 첫 번째 객체 속성 추출

<br />

#### 속성 지정

##### 속성 값을 입력해 속성 지정

- attr() 메서드의 매개변수에 속성이름, 속성 값 입력

```html
<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        // 이벤트를 연결합니다.
        $(document).ready(function () {
            // 속성을 지정합니다.
            $('img').attr('alt', 'jQuery 라이브러리를 사용한 속성 지정');
            $('img').attr('src', 'http://placehold.it/100x100');
            $('img').attr('width', '100');
        });
    </script>
</head>
<body>
    <img />
    <img />
    <img />
</body>
</html>

```

<br />

##### 객체를 입력해 속성 지정

- attr() 메서드의 매개변수에 객체 입력

```html
<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        // 이벤트를 연결합니다.
        $(document).ready(function () {
            // 속성을 지정합니다.
            $('img').attr({
                alt: 'jQuery 라이브러리를 사용한 속성 지정',
                src: 'http://placehold.it/100x100',
                width: 100
            });
        });
    </script>
</head>
<body>
    <img />
    <img />
    <img />
</body>
</html>

```



<br />

##### 함수를 이용해 속성 지정

- attr() 메서드의 매개변수에 속성 이름, 함수 입력
- 매개변수로 입력 된 콜백 함수에 index  객체가 순서대로 지정 되고, 반환 값을 속성에 적용

```html
<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        // 이벤트를 연결합니다.
        $(document).ready(function () {
            // 속성을 지정합니다.
            $('img').attr({
                alt: 'jQuery 라이브러리를 사용한 속성 지정',
                src: function (index) {
                    // 변수를 선언합니다.
                    var size = (index + 1) * 100;

                    // 리턴합니다.
                    return 'http://placehold.it/' + size + 'x100';
                }
            });
        });
    </script>
</head>
<body>
    <img />
    <img />
    <img />
</body>
</html>

```

##### 메서드 체이닝

```html
<script>
	$(document).ready(function () {
        $('ing').attr('alt', 'jQuery 라이브러리를 사용한 속성 지정')
        .attr('src', '[이미지 경로]').attr('width', '100')
    });
</script>
```

- $() 함수는 자기 자신을 반환
  - 따라서 연속하여 메서드 사용 가능

<br />

#### 스타일 조작

- 스타일을 조작할 때는 css() 메서드 사용
- attr()메서드와 같은 방법으로 사용
- 두 번째 매개 변수에 속성 값을 입력하여 조작

<br />

##### 속성 값을 입력

```javascript
$('.box').css('backgroundColor', 'blue');
$('body').css('background-Color','red');
$('.box').css('float','left');
$('.button').css('width',100);
$('.box').css('margin',10);
```

<br />

##### 객체를 입력

```html
<script>
$(document).ready(function () {
    $('.box').css({
        float : 'left',
            margin:10,
            width:100,
            height:100,
            backgroundColot:'red'
    });
});
</script>
```

<br />

##### 함수를 이용

```html
<script>
	$(document).ready(function () {
        var output = '';
        for (var i = 0; i<256; i++){
            output += '<div></div>';
        }
        document.body.innerHTML = output;
    	$('div').css({
            height:2,
            backgroundColor:function (i) {
                return 'rgb(' + i + ',' + i + ',' + i + ')';
            }
        })
    })
</script>
```

<br />

#### 글자 조작

- 문서 객체 내부의 글자를 조작 할 때 사용하는 메서드
- html() : 문서 객체 내부의 HTML 태그 조작하여 글자에 태그 적용 됨
- text() : 문서 객체 내부의 글자 조작일반 글자처럼 출력 됨

```html
<script>
	$(document).ready(function () {
        $('h1:nth-child(1)').text('<a href="#">text()</a>'');
        $('h1:nth-child(2)').html('<a href="#">html()</a>'');
    })
</script>
```

![image-20210224220655536](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210224220655536.png)

<br />

#### 클래스 조작

- 문서 객체의 class 속성 값을 띄어쓰기로 구분해 여러 개 입력 가능

```html
<p class="bold big italic">스타일 적용 본문 내용</p>
```

| 클래스 조작용 메서드 | 설명        |
| -------------------- | ----------- |
| addClass()           | 클래스 추가 |
| removeClass()        | 클래스 제거 |
| toggleClass()        | 클래스 전환 |

```html
<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <style>
        #box {
            width: 100px; height: 100px;
            background-color: red;
        }

        #box.hover {
            background-color: blue;
            border-radius: 50px;
        }
    </style>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        $(document).ready(function () {
            $('#box').hover(function () {
                // 스타일을 변경합니다.
                $('#box').addClass('hover');
            }, function () {
                // 스타일을 변경합니다.
                $('#box').removeClass('hover');
            });
        });
    </script>
</head>
<body>
    <div id="box"></div>
</body>
</html>
```

<br />

## 이벤트

#### 이벤트 연결

| 간단한     | 이벤트     | 연결      | 메서드    |          |
| ---------- | ---------- | --------- | --------- | -------- |
| blur       | focur      | focusin   | focusout  | load     |
| resize     | scroll     | unload    | click     | dbclick  |
| mousedown  | mouseup    | mousemove | mouseover | mouseout |
| mouseenter | mouseleave | change    | select    | submit   |
| keydown    | keypress   | keyup     | error     | ready    |

<br />

##### 간단한 방식으로 이벤트 연결 할 때 사용하는 양식

```html
$(selector).method(function (event) {});
```

<br />

##### 복합 이벤트 연결 메서드

```html
<!DOCTYPE html>
<html>
<head>
    <title>Animate Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        // 이벤트를 연결합니다.
        $(document).ready(function () {
            // 이벤트를 연결합니다.
            $('h1').hover(function () {
                // 색상을 변경합니다.
                $(this).css({
                    background: 'red',
                    color: 'white'
                });
            }, function () {
                // 색상을 제거합니다.
                $(this).css({
                    background: '',
                    color: ''
                });
            });
        });
    </script>
</head>
<body>
    <h1>Click</h1>
</body>
</html>
```



<br />

#### 이벤트 사용

- 연결 할 때 on(), 제거 할 때 off() 사용
- 사용자 정의 이벤트, 업데이트 하지 못한 이벤트는 반드시 on() 메서드를 사용하여 연결 해야 함

```css
$(selector).on(eventName, eventHandler);
//또는
$(selector).on({
    eventName_0: eventHandler_0,
            eventName_0: eventHandler_0,
            eventName_1: eventHandler_1,
            eventName_2: eventHandler_2,
});
```

- 여러 이벤트와 이벤트 핸들러  연결

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>jQuery Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        $(document).ready(function () {
            // 스타일 변경 및 이벤트를 연결
            $('#box').css({
                width: 100,
                height: 100,
                background: 'orange'
            }).on({
                click: function () {
                    $(this).css('background', 'red');
                },
                mouseenter: function () {
                    $(this).css('background', 'blue');
                },
                mouseleave: function () {
                    $(this).css('background', 'orange');
                }
            });
        });
    </script>
</head>
<body>
    <div id="box"></div>
</body>
</html>
```

<br />

#### 이벤트 제거와 유효성 검사

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>Event Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        // 이벤트를 연결합니다.
        $(document).ready(function () {
            // 이벤트를 연결합니다.
            $('form').submit(function (event) {
                // input 태그의 값을 추출합니다.
                var value = $('input').val();

                // 유효성을 검사합니다.
                if (value.replace(/[가-힣]/g, '').length == 0) {
                    // 유효성 검사 통과
                    alert('과정을 진행합니다.');
                } else {
                    // 유효성 검사 실패
                    alert('한글 이름을 입력해주세요.');
                    event.preventDefault();
                }
            });
        });
    </script>
</head>
<body>
    <form>
        <label>이름</label>
        <input type="text" />
        <input type="submit" />
    </form>
</body>
</html>

```

<br />

## 시각 효과

#### 시각효과 메서드

| 메서드            | 설명                                               |
| ----------------- | -------------------------------------------------- |
| show(속도)        | 문서 객체가 커지며 표시                            |
| hide(속도)        | 문서 객체가 작아지며 사라짐                        |
| toggle(속도)      | show() 메서드와 hide() 메서드를 번갈아 실행        |
| slideDown(속도)   | 문서 객체가 슬라이드 효과와 함께 표시              |
| slideUp(속도)     | 문서 객체가 슬라이드 효과와 함께 사라짐            |
| slideToggle(속도) | slideDown()메서드와 slideUp() 메서드를 번갈아 실행 |
| fadeIn(속도)      | 문서 객체가 선명해지며 표시                        |
| fadeOut(속도)     | 문서 객체가 흐려지며 사라짐                        |
| fadeToggle(속도)  | fadIn() 메서드와 fadeOut() 메서드를 번갈아 실행    |

<br />

#### 속도 문자열

- 속도 입력하지 않을 시 기본 값 500밀리 초 적용

| 속도 문자열 | 설명           |
| ----------- | -------------- |
| slow        | 600밀리초 속도 |
| normal      | 500밀리초 속도 |
| fast        | 400밀리초 속도 |

<br />

```html
<script>
	$(document).ready(function () {
        $('button').click(funcion () {
        	$('.page').fadeToggle('slow');
       });
    });
</script>
```

<br />

#### 애니메이션 효과

- animate() 메서드를 세 가지 형태로 사용하여 문서 객체에 애니메이션 효과 부여

```html
$(selector).animate(속성 객체);
$(selector).animate(속성 객체, 시간);
$(selector).animate(속성 객체, 시간, 콜백 함수);
```

<br />

##### 메서드 체이닝 애니메이션

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>Animate Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        $(document).ready(function () {
            // 스타일을 변경합니다.
            $('#box').css({
                width: 100,
                height: 100,
                background: 'red',
                position: 'absolute',
                left: 10,
                top: 10
            }).animate({
                width: 300,
                opacity: 0.5
            }, 500).animate({
                opacity: 1,
                left: 100,
                top: 200
            }, 500);
        });
    </script>
</head>
<body>
    <div id="box"></div>
</body>
</html>
```

<br />

##### 상대적 증가

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>Animate Basic</title>
    <script src="http://code.jquery.com/jquery-3.1.0.js"></script>
    <script>
        $(document).ready(function () {
            // 스타일을 변경합니다.
            $('#box').css({
                width: 100,
                height: 100,
                background: 'red',
                position: 'absolute',
                left: 10,
                top: 10
            }).animate({
                height: '+=300',
                width: '-=50',
                left: '+=300',
                opacity: 0.5
            }, 500);
        });
    </script>
</head>
<body>
    <div id="box"></div>
</body>
</html>
```

<br />

##### 애니메이션 딜레이

- animate() 메서드 또는 delay()메서드를 사용해 애니메이션을 큐의 형태로 예약하여 사용
- delay()
  - 특정 시간만큼 정지
  - animate()메서드 사이에 입력하여 사용

```html
$('h1').animate({}).delay(1000).animate({});
```

- stop()
  - 완전히 정지
  - 매개변수로 bool 값을 입력
  - clearQueue 매개변수의 기본 값은 false로 현재 실행 중인 애니메이션만 정지
    - true를 입력하면 예약 된 모든 애니메이션 제거
  - goToEnd 매개변수는 애니메이션을 중지 할 때 어떤 상태로 종료 할 지 지정
    - 지정한 최종 수치로 변경되어 정지, 지정하지 않으면 호출 시점에 정지 됨