## 문서 객체 모델

- 문서 객체 (Document Object)
  - HTML 태그(h1, p 등)를 자바스크립트에서 사용 할 수 있는 객체로 만든 것
  - 문서 객체 조작은 HTML 태그 조작과 같은 의미
- 웹 브라우저는 HTML 페이지를 읽으면서 태그의 포함 관계에 따라 문서 객체를 트리 형태로 만듬
- 노드 (Node)
  - 트리의 각 요소
  - 요소 노드(Element node)
    - h1 태그, script 태그 등 요소를 생성하는 노드
  - 텍스트 노드(text node)
    - 화면에 출력되는 문자열
    - h1처럼 텍스트 노드를 갖는 태그도 있지만 br, hr, img 등 없는 태그도 있음

![img](https://eloquentjavascript.net/1st_edition/img/html.png)

<br />

### DOM

웹 브라우저는 웹 페이지를 실행할 때 먼저 HTML 파일을 분석한 후 화면에 표시한다.

웹 브라우저가 HTML 파일을 분석하고 출력하는 방식을 문서 객체 모델(Document Object Model)이라고 한다.

- 정적 생성
  - 웹 페이지를 처음 실행할 때 HTML 태그로 적힌 문서 객체를 생성하는 것
- 동적 생성
  - 웹 페이지를 실행 중에 자바스크립트를 사용해 문서 객체를 생성하는 것

<br />

### 웹 페이지 실행 순서

- 웹 브라우저는 HTML 코드를 위에서 아래 순서로 실행 한다.
  - 따라서 순서대로 작성하거나 이벤트 기능을 이용해야 한다.

<br />

## 문서 객체 선택

- 이미 존재하는 HTML 태그를 자바스크립트에서 문서 객체로 변환하는 것을 '문서 객체를 선택한다'라고 표현

<br />

##### 문서 객체 선택 메서드

| 구분         | 메서드                                 | 설명                        |
| ------------ | -------------------------------------- | --------------------------- |
| 1개 선택     | document.getElementByid(아이디)        | 아이디로 1개 선택           |
| 1개 선택     | document.querySelector(선택자)         | 선택자로 1개 선택           |
| 여러 개 선택 | document.getElementByName(이름)        | name 속성으로 여러 개 선택  |
| 여러 개 선택 | document.getElementByClassName(클래스) | class 속성으로 여러 개 선택 |
| 여러 개 선택 | document.querySelectorAll(선택자)      | 선택자로 여러 개 선택       |

- getTelementByid(아이디) 
  - 메서드 사용 시 id 속성이 중복되면 객체 선택 시 문제가 발생함
- querySelector(선택자)
  - 문서 객체 하나를 선택하기 때문에 같은 선택자가 여러개면 가장 먼저 등장하는 것 선택
- querySelectorAll(선택자)
  - 문서 객체 여러개 선택하여 배열로 반환
  - 모든 객체의 속성을 조작 할 경우 반복문을 함께 사용 함

```html
﻿<!DOCTYPE html>
<html>
<head>
    <title>DOM Basic</title>
    <script>
        // 이벤트를 연결합니다.
        window.onload = function () {
            // 문서 객체를 선택합니다.
            var headers = document.querySelectorAll('h1');

            for (var i = 0; i < headers.length; i++) {
                // 변수를 선언합니다.
                var header = headers[i];

                // 문서 객체를 조작합니다.
                header.style.color = 'orange';
                header.style.background = 'red';
                header.innerHTML = 'From JavaScript';
                }
            
            var header = document.querySelector('h2');

	            // 문서 객체를 조작합니다.
    	        header.style.color = 'yellow';
        	    header.style.background = 'green';
            	header.innerHTML = 'From JavaScript';
                
            var header = document.getElementById('header');

            // 문서 객체를 조작합니다.
            header.style.color = 'black';
            header.style.background = 'gray';
            header.innerHTML = 'From JavaScript';
        };
            
    </script>
</head>
<body>
    <h1>Header</h1>
    <h2>Header</h2>
    <h3 id="header">Header</h3>
</body>
</html>
```

![image-20210225002836254](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210225002836254.png)

<br />

## 문서 객체 조작

##### SPA(Single Page Application)

- 처음 웹 페이지를 읽어 들일 때 한 번만 읽어 들임
  - 처음에 틀만 읽고 이후에 자바스크립트 문서 객체를 조작하여 내용을 후입력

<br />

### 글자 조작

| 속성        | 설명                                                       |
| ----------- | ---------------------------------------------------------- |
| textContent | 문서 객체 내부 글자를 순수 텍스트 형식으로 가져오도록 변경 |
| innerHTML   | 문서 객체 내부 글자의 HTML 태그를 반영해 가져오도록 변경   |

- textContent를 사용한 경우
  - `<h1>Header - 0</h1><h2>Header - 1</h2>`

- innerHTML을 사용한 경우

  - <h1>Header - 0</h1><h2>Header - 1</h2>

    

<br />

### 스타일 조작

- 자바스크립트로 CSS 속성 값을 추가, 제거, 변경 가능
  - 문서 객체의 style 속성을 변경하면 됨
  - 자바스크립트에서는 특수 문자 `-` 를 식별자에서 사용할 수 없으므로 연결된 단어의 첫 글자를 대문자로 변경하여 사용
  - background-color -> backgrounColor

<br />

##### 스타일 식별자 변환

| 스타일시트의 스타일 속성 | 자바스크립트의 스타일 식별자 |
| ------------------------ | ---------------------------- |
| background-image         | backgroundImage              |
| background-color         | backgroundColor              |
| box-sizing               | boxSizing                    |
| list-style               | listStyle                    |

<br />

##### 그라디언트 생성 예제

```html
<!DOCTYPE html>
<html>
<head>
    <title>DOM Basic</title>
    <script>
        // 이벤트 연결
        window.onload = function () {
            // 문서 객체 추가
            var output = '';
            for (var i = 0; i < 256; i++) {
                output += '<div></div>';
            }
            document.body.innerHTML = output;

            // 문서 객체 선택
            var divs = document.querySelectorAll('div');
            for (var i = 0; i < divs.length; i++) {
                // 변수 선언
                var div = divs[i];

                // 스타일 적용
                div.style.height = '2px';
                div.style.background = 'rgb(' + i + ',' + i + ',' + i + ')';
            }
        };
    </script>
</head>
<body>
    
</body>
</html>
```

<br />

### 속성 조작

| 메서드                           | 설명      |
| -------------------------------- | --------- |
| setAttribute(속성 이름, 속성 값) | 속성 지정 |
| getAttribute(속성 이름)          | 속성 추출 |

<br />

##### 웹 표준에서 지정한 속성 추출

```html
image.src = 'image.png'
alert(image.src)
```

<br />

##### 웹 표준에서 지정하지 않은 속성 추출(사용자 지정 속성)

```html
<!DOCTYPE html>
<html>
<head>
    <title>DOM Basic</title>
    <script>
        // 이벤트를 연결합니다.
        window.onload = function () {
            // 속성을 지정합니다.
            document.body.setAttribute('data-custom', 'value');

            // 속성을 추출합니다.
            var dataCustom = document.body.getAttribute('data-custom');
            alert(dataCustom);
        };
    </script>
</head>
<body>

</body>
</html>
```

![image-20210225005756338](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210225005756338.png)

<br />

##### 동적으로 객체의 속성 조작

```html
<!DOCTYPE html>
<html>	
	<head>
		<title>Clock</title>
		<script>
			// 이벤트를 연결합니다.
			window.onload = function () {
				// 변수를 선언합니다.
				var clock = document.getElementById('clock');

				// 1초마다 함수를 실행합니다.
				setInterval(function () {
					var now = new Date();
					clock.innerHTML = now.toString();

				}, 1000);
			};
		</script>
	</head>
	<body>
		<h1 id="clock"></h1>
	</body>
</html>
```

![image-20210225005902811](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210225005902811.png)

<br />

## 이벤트

#### 자바스크립트에서 기본적으로 지원하는 이벤트

- 마우스, 키보드 이벤트
- HTML 프레임, HTML 입력 양식 이벤트
- 사용자 인터페이스 이벤트
- 구조 변화 이벤트
- 터치 이벤트

<br />

```html
window.onload = function () {}

// 코드에서 onload를 '이벤트 속성'이라고 부름
// on을 제외한 load를 '이벤트 이름' 또는 '이벤트 타입'이라고 부름
// 이벤트 속성에 넣는 함수를 '이벤트 리스너' 또는 '이벤트 핸들러'라고 부름
```

<br />

### 이벤트 연결

- 문서 객체에 이벤트를 연결하는 방식을 이벤트 모델(Event Model)이라고 함

  - DOM레벨에 따라 구분하며 가각 두 가지로 다시 나뉨

  

  

- DOM 레벨 0

  - 쉬워서 널리 사용 됨
  - 이벤트를 중복해서 연결하지 못하는 단점
    - 인라인 이벤트 모델
    - 고전 이벤트 모델

- DOM 레벨 2

  - 웹 브라우저 종류에 따라 연결하는 방식이 다름
    - jQuery 라이브러리 등을 사용해 극복 가능
  - 이벤트를 중복해서 연결 가능
    - 마이크로소프트 인터넷 익스플로러 이벤트 모델
    - 표준 이벤트 모델

<br />

##### 인라인 이벤트 모델

- HTML 태그 내부에 자바스크립트 코드를 넣어 이벤트를 연결하는 방식

```html
<body>
    <button onclic="alert('click')">
        버튼
    </button>
</body>
```

<br />

##### script 태그에 인라인 이벤트 모델 사용

```html
<head>
    <tilte>타이틀</tilte>
    <script>
        function buttonClick() {
            alert('click');
        }
    </script>
</head>
```

<br />

##### 고전 이벤트 모델

```html
<!DOCTYPE html>
<html>
<head>
    <title>Traditional Event</title>
    <script>
        // 이벤트를 연결합니다.
        window.onload = function () {
            // 문서 객체를 선택합니다.
            var button = document.getElementById('button');

            // 이벤트를 연결합니다.
            button.onclick = function () {
                alert('click');
            };
        };
    </script>
</head>
<body>
    <button id="button">버튼</button>
</body>
</html>
```

<br/>

##### 이벤트 발생 객체 예제

```html
<!DOCTYPE html>
<html>
<head>
    <title>Traditional Event</title>
</head>
<body>
    <button id="button">버튼 - </button>
    <script>
        // 이벤트 연결, body 태그 아래에 스크립트를 넣어 window.onload 사용하지 않음
        document.getElementById('button').onclick = function () {
            this.textContent = this.textContent + '★';	//자기 자신의 글자를 변경
        };
    </script>
</body>
</html>
```



<br/>

### 이벤트 사용

- 기본 이벤트 (Default Event)
  - a 태그를 클릭하면 href 속성에 입력한 위치로 이동하거나 form 태그로 생성한 <제출>버튼을 누르면 자동으로 입력 양식을 제출하는 등 특정 태그가 지닌 기본적인 이벤트를 말함

- 기본 이벤트를 제거 할 때 이벤트 리스너의 반환 값으로 false를 입력

```html
<!DOCTYPE html>
<html>
<head>
    <title>Traditional Event - Default Event</title>
    <script>
        // 이벤트 연결
        window.onload = function () {
            // 문서 객체 선택
            var button = document.getElementById('button');

            // 이벤트 연결
            button.onclick = function () {
                // 기본 이벤트 제거
                return false;
            };
        };
    </script>
</head>
<body>
    <a id="button" href="http://hanb.co.kr">버튼</a>
</body>
</html>
```

