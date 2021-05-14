## 1. 리액트 환경 설정

- [NPM](https://nodejs.org/en/)
  - nodeJS로 만들어진  앱 들을 명령어 기반으로 쉽게 받을 수 있도록 제공
  - 때문에 nodeJS 설치 필요
    - LTS : 안정화 된 버전

#### 설치 및 버전 확인

```shell
npm -v
```

<br/>

- Create react app 설치

```shell
npm install -g create-react-app
```

<br/>

설치 및 버전 확인

```shell
create-react-app --version
```

<br/>

#### Window Power Shell에서 실행 시

```shell
PS C:\Users\user> create-react-app -v
create-react-app : 이 시스템에서 스크립트를 실행할 수 없으므로 C:\Users\user\AppData\Roaming\npm\create-react-app.ps1
파일을 로드할 수 없습니다. 자세한 내용은 about_Execution_Policies(https://go.microsoft.com/fwlink/?LinkID=135170)를 참
조하십시오.
위치 줄:1 문자:1
+ create-react-app -v
+ ~~~~~~~~~~~~~~~~
    + CategoryInfo          : 보안 오류: (:) [], PSSecurityException
    + FullyQualifiedErrorId : UnauthorizedAccess
```

위와 같은 에러 발생 시

- window powershell 관리자로 실행 후 `Get-ExecutionPolicy` 명령어 입력하여 확인
  - Restricted로 나오면 RemoteSigned로 변경

```shell
PS C:\WINDOWS\system32> Get-ExecutionPolicy
Restricted
PS C:\WINDOWS\system32> Set-ExecutionPolicy RemoteSigned

실행 규칙 변경
실행 정책은 신뢰하지 않는 스크립트로부터 사용자를 보호합니다. 실행 정책을 변경하면 about_Execution_Policies 도움말
항목(https://go.microsoft.com/fwlink/?LinkID=135170)에 설명된 보안 위험에 노출될 수 있습니다. 실행 정책을
변경하시겠습니까?
[Y] 예(Y)  [A] 모두 예(A)  [N] 아니요(N)  [L] 모두 아니요(L)  [S] 일시 중단(S)  [?] 도움말 (기본값은 "N"): y
PS C:\WINDOWS\system32> Get-ExecutionPolicy
RemoteSigned
PS C:\WINDOWS\system32>
```

<br/>

[아래 실행 정책 내용 출처](https://talsu.net/?p=834)

> Windows PowerShell 실행 정책은 다음과 같습니다.
>
> 기본 정책은 “Restricted”입니다.
>
> **Restricted**
> – 기본 실행 정책입니다.
>
> – 개별 명령을 허용하지만 스크립트를 실행하지 않습니다.
>
> – 서식 지정 및 구성 파일(.ps1xml), 모듈 스크립트 파일(.psm1), Windows
> PowerShell 프로필(.ps1) 등의 모든 스크립트 파일을 실행할 수 없습니다.
>
> **AllSigned**
> – 스크립트를 실행할 수 있습니다.
>
> – 로컬 컴퓨터에 작성하는 스크립트를 포함하여 모든 스크립트 및 구성 파일에 신뢰된 게시자가
> 서명해야 합니다.
>
> – 신뢰된 게시자나 신뢰되지 않은 게시자로 아직 분류하지 않은 게시자의 스크립트를 실행하기 전에
> 메시지를 표시합니다.
>
> – 인터넷 이외의 다른 소스에서 가져온 서명되지 않은 스크립트를 실행하거나 서명되었지만 악의적인
> 스크립트를 실행할 위험이 있습니다.
>
> **RemoteSigned**
> – 스크립트를 실행할 수 있습니다.
>
> – 전자 메일과 인스턴트 메시징 프로그램을 포함하여 인터넷에서 다운로드하는 스크립트와 구성
> 파일에는 신뢰된 게시자의 디지털 서명이 필요합니다.
>
> – 이미 실행한 스크립트와 로컬 컴퓨터에 작성한(인터넷에서 다운로드하지 않음) 스크립트에는
> 디지털 서명이 필요 없습니다.
>
> – 서명되었지만 악의적인 스크립트를 실행할 위험이 있습니다.
>
> **Unrestricted**
> – 서명되지 않은 스크립트를 실행할 수 있습니다. 이 경우 악의적인 스크립트를 실행할 위험이
> 있습니다.
>
> – 인터넷에서 다운로드한 스크립트와 구성 파일을 실행하기 전에 사용자에게 경고합니다.
>
> **Bypass**
> – 아무 것도 차단되지 않으며 경고나 메시지가 표시되지 않습니다.
>
> – 이 실행 정책은 Windows PowerShell 스크립트가 대규모 응용 프로그램에 기본 제공되는 구성 또는
> 고유의 보안 모델을 가진 프로그램이 Windows PowerShell을 기초로 하는 구성을 위해
> 설계되었습니다.
>
> **Undefined**
> – 현재 범위에 설정된 실행 정책이 없습니다.
>
> – 모든 범위의 실행 정책이 Undefined인 경우 적용되는 실행 정책은 기본 실행 정책인
> Restricted입니다.
>
> 참고: UNC(범용 명명 규칙) 경로를 인터넷 경로와 구별하지 않는 시스템에서는 UNC 경로로 식별되는
> 스크립트를 RemoteSigned 실행 정책으로 실행하는 것이 허용되지 않을 수 있습니다.

<br/>

- npm 은 프로그램 설치

- npX 는 다운로드하여 실행, 종료 시 삭제 됨
   - 용량 차지 안하고 매번 최신 버전 유지 가능

<br/>

### 리액트 개발환경 설정

- 개발 환경 세팅 할 디렉토리 생성
  - react로 이름 설정하면 안됨
- 해당 디렉토리로 경로로 이동하여 

```shell
create-react-app .
```

 -> `한칸 띄우고 .`은 현재 디렉토리의 상대경로

- 성공시 아래와 같은 문구가 나온다

```shell
Success! Created react_practice at [디렉토리 경로]
Inside that directory, you can run several commands:

  npm start
    Starts the development server.

  npm run build
    Bundles the app into static files for production.

  npm test
    Starts the test runner.

  npm run eject
    Removes this tool and copies build dependencies, configuration files
    and scripts into the app directory. If you do this, you can’t go back!

We suggest that you begin by typing:

  cd [디렉토리 경로]
  npm start

Happy hacking!
```

<br/>

- 해당 디렉토리로 가면 세 가지 폴더가 생성 되어 있다.
  - node_modules
  - public
    - index.html이 들어 있음
  - src
    - index.js, App.js 등 소스들이 모여 있는 디렉토리

<br/>

### VScode에서 Terminal을 켜서 실습 진행

- `view` -> `terminal`

- 디렉토리 경로로 이동하여 리액트 실행

```shell
npm run start
```

- 실행 시 아래와 같이 실행중인 리액트 확인 가능한 URL 주소를 보여주고 브라우저에 리액트가 실행 됨

```shell
Compiled successfully!

You can now view react_practice in the browser.  

  Local:            http://localhost:[포트번호]        
  On Your Network:  http://[IP주소]:[포트번호]

Note that the development build is not optimized.
To create a production build, use npm run build. 
```

- 종료는 `CTRL + C`

<br/>

- 실행 후 index.html에 들어가면 아래와 같은 root 태그가 있음

```html
  <body>
    <noscript>You need to enable JavaScript to run this app.</noscript>
    <div id="root"></div>
    <!--
      This HTML file is a template.
      If you open it directly in the browser, you will see an empty page.

      You can add webfonts, meta tags, or analytics to this file.
      The build step will place the bundled scripts into the <body> tag.

      To begin the development, run `npm start` or `yarn start`.
      To create a production bundle, use `npm run build` or `yarn build`.
    -->
  </body>
```

- 리액트에서 생성되는 컴포넌트 들은 위 root로 들어오기로 약속 되어 있음

<br/>

### 클래스 형식으로 진행하기 위해 App.js 수정

```javascript
// import React from 'react';
import React, { Component } from 'react';
import './App.css';

// function App(){
class App extends Component {
// render 없음
render(){
  return (
    <div className="App">
  
    </div>
  );
}
}

export default App;
```

- 주석은 기존 함수형일 때의 코드

<br/>

#### index.js

```javascript
import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

ReactDOM.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
```

- root라는 속성을 찾아 해당 문서에 App을 렌더링 해준다.
  - 이렇게 render 되는 App은 import 되는 App과 동일 한 것으로 from ./App.js는  현재 디렉토리의 App.js를 의미한다.

<br/>

```javascript
import React, { Component } from 'react';
import './App.css';

class App extends Component {
render(){
  return (
    <div className="App">
      // App div 태그 안의 내용이 렌더링 된다.
      Hello, React!!!
    </div>
  );
}
}

export default App;
```

<br/>

- index.html은 `import './index.css';`을 통해 css를 적용 한다. 
  - 수정한 내용은 자동 적용 된다.

<br/>

### 배포

- 그냥 올리면 (강력한 새로 고침) 기본 1.7MB의 용량이 올라간다.
  - 때문에 프로덕트 앱을 만들기 위해 빌드를 진행 한다. (불필요한 정보/코드 삭제)

```shell
npm run build
```

<br/>

#### 위 명령어 실행 시 아래와 같이 진행 됨

```shell
> react-scripts build

Creating an optimized production build...
Compiled successfully.

File sizes after gzip:

  41.83 KB  build\static\js\2.adebfcb2.chunk.js
  1.63 KB   build\static\js\3.451cc43a.chunk.js
  1.17 KB   build\static\js\runtime-main.478700d8.js
  521 B     build\static\js\main.1dbc3403.chunk.js
  72 B      build\static\css\main.bab0b925.chunk.css

The project was built assuming it is hosted at /.
You can control this with the homepage field in your package.json.    

The build folder is ready to be deployed.
You may serve it with a static server:

  npm install -g serve
  serve -s build

Find out more about deployment here:

  https://cra.link/deployment
```

- 빌드를 하면 build라는 디렉토리가 생성 되고 내부에 

![build](https://github.com/ktae23/til/react/imgs/build.png)

- 위와 같은 필요 파일들만 남게 됨
- 또한 아래와 같이 실행에 불필요한 내용이 사라짐

![build](https://github.com/ktae23/til/react/imgs/build_js.png)

<br/>

- server를 받아서 실행할 수 있음

```shell
npm install -g serve
# 다운로드 후
serve -s build
# build 폴더를 디렉토리로 설정하여 서버에 올리겠다

또는

ntx serve -s build
# ntx를 이용해 일회용 서버를 다운로드 받아 사용 할 수 있음
```

- 서버 실행 시 아래와 같이 접속 가능한 URL을 알려 준다.

```shell

   ┌──────────────────────────────────────────────────┐
   │                                                  │
   │   Serving!                                       │
   │                                                  │
   │   - Local:            http://localhost:[포트번호] │
   │   - On Your Network:  http://[IP주소]:[포트번호]   │
   │                                                  │
   │   Copied local address to clipboard!             │
   │                                                  │
   └──────────────────────────────────────────────────┘

```

- 위에 올라온 주소로 접속하면 기존 1.7MB의 용량이 147 kB로 줄어들어 있다.