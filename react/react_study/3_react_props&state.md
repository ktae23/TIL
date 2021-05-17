### State?

- Component
  - 사용자 입장에서 중요한 것

- State
  - Component 내부에서 사용하는, 밖으로 노출되어서는 안되는 분리 된 데이터

<br/>

### render 함수에 앞서 생성자를 이용한 state 값 초기화

### App.js

```jsx
import React, { Component } from 'react';
import './App.css';
import TOC from './componet/TOC';
import Subject from './componet/Subject';
import Content from './componet/Content';
 

class App extends Component {
  constructor(props){
    super(props);
    this.state={
        subject:{title:"WEB",sub:"World Wide Web!"},
        contetns:[
            {id:1, title:"HTML", desc:"HTML is for information."},
            {id:2, title:"CSS", desc:"CSS is for design"},
            {id:3, title:"JavaScript", desc:"JavaScript is for interactive"}
        ]
    }
  }
  render(){
    return (
      <div className="App">
        Hello, React!!!
        <Subject 
            title={this.state.subject.title} 
            sub={this.state.subject.sub}></Subject>
        <TOC data={this.state.contents}></TOC>
        <Content title="HTML" desc="HTML is HyperText Markup Language."></Content>
      </div>
    );
  }
}

export default App;
```

- 상위 컴포넌트의 state를 하위 컴포넌트의 props의 값으로 전달하여 사용

<br/>

#### index.js

```jsx
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

- 인덱스에서는 \<App />으로 호출만 하기 때문에 내부적으로 state 값이 어떤 정보가 사용되는지 알 수 없음. 
  - 이러한 정보 은닉이 좋은 사용성을 만드는 핵심

<br/>

### TOC(table of contents) 변경

### TOC.js

```jsx
import React, { Component } from 'react';

class TOC extends Component {

    render() {
        var lists=[];
        var data = this.props.data;
        var i = 0;
        while(i<data.length){
        lists.push(<li key={data[i].id}><a href={"/content/" + data[i].id}>{data[i].title}</a></li>);
            i = i+1;
    }
        return(
          <nav>
              <ul>
                   {lists}
              </ul>
          </nav>
        );
      }
    }

    export default TOC;
```

<br/>

### Key

- 반복 생성 할 때는 `key`라는 `props`를 가지고 있어야 한다.
  - 이는 jsx 내부에서 사용하는 값
  - 다른 것과 구분 할 수 있는 식별자를 입력하면 됨

