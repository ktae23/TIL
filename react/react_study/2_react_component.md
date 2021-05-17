### 순수 HTML 파일로 Component 만들기

``` html
<html>
    <body>
        <header>
            <h1>WEB</h1>
            worl wide web!
        </header>

        <nav>
            <ul>
                <li><a href="1.html">HTML</a></li>
                <li><a href="2.html">CSS</a></li>
                <li><a href="3.html">JavaScript</a></li>
            </ul>
        </nav>

        <article>
            <h2>HTML</h2>
            HTML is HyperText Markup Language.
        </article>
    </body>
</html>
```

- 위 HTML을 쪼개서 Component로 만들려 한다.

<br/>

#### \<header> 태그를 \<subject> 컴포넌트로 만들기

```jsx
class Subject extends Component {
// function render(){} 여야 하지만 javascript의 최신 스펙이 들어간 class 내부에서는 생략
  render() {
    return(
      // 컴포넌트를 만들 때는 반드시 하나의 최상위 태그만 사용해야만 함
      <header>
        <h1>WEB</h1>
        worl wide web!
      </header>
    );
  }

```

- \<header>태그를 잘라내어 컴포넌트 내부에 작성
  - 실행 시 해당 html을 넣어줌
  - 위 코드는 JSX(페이스북에서 만든 언어)로 유사 자바스크립트이다. 때문에 태그나 문자 사용에 용이하다.

<br/>

#### pure.html 내용을 모두 Component로 바꿔보자

<br/>

```jsx
class Subject extends Component {
// function render(){} 여야 하지만 javascript의 최신 스펙이 들어간 class 내부에서는 생략
  render() {
    return(
      // 컴포넌트를 만들 때는 반드시 하나의 최상위 태그만 사용해야만 함
      <header>
        <h1>WEB</h1>
        worl wide web!
      </header>
    );
  }
}

class TOC extends Component {
  render() {
      return(
        <nav>
            <ul>
                <li><a href="1.html">HTML</a></li>
                <li><a href="2.html">CSS</a></li>
                <li><a href="3.html">JavaScript</a></li>
            </ul>
        </nav>
      );
    }
  }

class Content extends Component {
    render() {
        return(
            <article>
            <h2>HTML</h2>
            HTML is HyperText Markup Language.
            </article>
        );
    }
}
  
class App extends Component {
  render(){ 
    return (
      <div className="App">
        Hello, React!!!
        <Subject></Subject>
        <TOC></TOC>
        <Content></Content>
      </div>
    );
  }
}
```

- 이제 App 컴포넌트 안에 위의 컴포넌트들이 들어가기 때문에 pure.html의 내용이 그대로 렌더링 되어 나오게 된다.

<br/>

### Component and PROPS

- Attribute와 같은 역할

```jsx
import React, { Component } from 'react';
import './App.css';

class Subject extends Component {
// function render(){} 여야 하지만 javascript의 최신 스펙이 들어간 class 내부에서는 생략
  render() {
    return(
      // 컴포넌트를 만들 때는 반드시 하나의 최상위 태그만 사용해야만 함
      <header>
        <h1>{this.props.title}</h1>
        {this.props.sub}
      </header>
    );
  }
}

class TOC extends Component {
  render() {
      return(
        <nav>
            <ul>
                <li><a href="1.html">HTML</a></li>
                <li><a href="2.html">CSS</a></li>
                <li><a href="3.html">JavaScript</a></li>
            </ul>
        </nav>
      );
    }
  }

class Content extends Component {
  render() {
    return(
      <article>
      <h2>{this.props.title}</h2>
      {this.props.desc}
      </article>
    );
  }
}
  

class App extends Component {
  render(){ 
    return (
      <div className="App">
        Hello, React!!!
        <Subject title="WEB" sub="world wide web!"></Subject>
        <TOC></TOC>
        <Content title="HTML" desc="HTML is HyperText Markup Language."></Content>
      </div>
    );
  }
}

export default App;

```

- props으로 입력한 값에 따라 출력 값이 달라 짐

<br/>

### Component 외부 파일로 분리하기

- 각각의 컴포넌트를 외부 파일로 분리한 뒤 불러오기
  - 각각의 컴포넌트를 js파일로 작성 한다. 이 때 react 임포트를 하고 `export default [파일이름]`

#### 예시

```jsx
import React, { Component } from 'react';

class Subject extends Component {
    // function render(){} 여야 하지만 javascript의 최신 스펙이 들어간 class 내부에서는 생략
      render() {
        return(
          // 컴포넌트를 만들 때는 반드시 하나의 최상위 태그만 사용해야만 함
          <header>
            <h1>{this.props.title}</h1>
            {this.props.sub}
          </header>
        );
      }
    }

export default Subject;
```

<br/>

#### App.js 컴포넌트 분리 결과

```jsx
import React, { Component } from 'react';
import './App.css';
import TOC from './componet/TOC';
import Subject from './componet/Subject';
import Content from './componet/Content';
 

class App extends Component {
  render(){ 
    return (
      <div className="App">
        Hello, React!!!
        <Subject title="WEB" sub="world wide web!"></Subject>
        <TOC></TOC>
        <Content title="HTML" desc="HTML is HyperText Markup Language."></Content>
      </div>
    );
  }
}

export default App;
```

