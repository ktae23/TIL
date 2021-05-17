### Event

현재 어떤 페이지인지 구분하기 위해 state에 mode라는 값을 줌

- 리액트에서는 Props나 State의 값이 변경 되면 해당 State를 가지고 있는 Component의 Render 함수가 다시 호출 됨
  - 이에 따라 Render 함수 하위의 Component의 Render 함수들도 모두 다시 호출하여 화면을 다시 그려 줌
- 이를 이용해 mode의 값에 따라 render를 다르게 호출 하도록 코드를 작성

<br/>

#### onClick

- original javascript에서는 onclick이지만

```html
<a href="/" onclick=""></a>
```

- jsx에서는 onClick이다.

```jsx
<a href="/" onClick={}></a>
```

<br/>

#### 태그의 기본 기능 제한하기

```jsx
onClick={function(e){
    e.preventDefault();
    ...
}}
```

- function의 첫번째 인자로 event를 받아 오기 때문에 event를 받아 기본 기능 제한, target 확인 등을 할 수 있다.

<br/>

#### Event에서 State 바꾸기

- 이미 생성 된 Constructor의 state를 변경 할 때는 this.가 아닌 `setState`함수를 이용하여 값을 변경해 주어야 한다.

```jsx
onClick={function(e){
    e.preventDefault();
    this.setState({
        mode:'welcome'
    });
}.bind(this)}
// .bind 함수를 통해 this와 component를 엮어 주어야 this 호출이 가능함
```

<br/>

### Component Event

- 컴포넌트를 이용하여 사용자 이벤트 제작

```jsx
<Subject 
    title={this.state.subject.title} 
    sub={this.state.subject.sub}
    // 페이지가 변경 되었을 때 mode를 'welcome'으로 세팅하는 함수 정의
    onChangePage={function(){
        this.setState({mode:'welcome'});
    }.bind(this)}>
</Subject>
```

- 위 컴포넌트에서 정의한 함수는 props를 통해 호출하여 사용 할 수 있음

```jsx
<header>
    <a href="/" onClick={function(e){
            e.preventDefault();
            // 컴포넌트를 통해 전달 된 props중 onChangePage 함수를 호출하여 사용
            this.props.onChangePage();
        }.bind(this)}>
        <h1>{this.props.title}</h1></a>
    {this.props.sub}
</header>
```

<br/>

#### 선택 된 정보 보여주기

##### App.js

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
      mode:"read",
      selected_content_id:2,
      subject:{title:"WEB",sub:"World Wide Web!"},
      welcome:{title:'Welcome', desc:'Hello, React!'},
      contents:[
          {id:1, title:"HTML", desc:"HTML is for information."},
          {id:2, title:"CSS", desc:"CSS is for design"},
          {id:3, title:"JavaScript", desc:"JavaScript is for interactive"}
      ]
    }
  }
  render(){
    var _tilte, _desc = null;

    if(this.state.mode === 'welcome'){
      _tilte = this.state.welcome.title;
      _desc = this.state.welcome.desc;

    }else if(this.state.mode === 'read'){
      var i = 0;

      while(i < this.state.contents.length){
        var data = this.state.contents[i];
			// 선택 된 컨텐트의 id와 일치할 경우 세팅 후 반복문 종료
        if(data.id === this.state.selected_content_id){
          _tilte = data.title;
          _desc = data.desc;
          break;
        }
        i = i + 1;
      }
    }
    return (
      <div className="App">
        <Subject 
          title={this.state.subject.title} 
          sub={this.state.subject.sub}
          onChangePage={function(){
            this.setState({mode:'welcome'});
          }.bind(this)}>
        </Subject>

        <TOC 
          onChangePage={function(id){
            this.setState({
              mode:'read',
              // 인자로 받은 id를 정수형으로 변환한 뒤 state 설정
              selected_content_id:Number(id)
            });
          }.bind(this)}
          data={this.state.contents}>
        </TOC>

        <Content 
          title={_tilte} desc={_desc}>
        </Content>
      </div>
    );
  }
}

export default App;

```

<br/>

##### TOC.js

```jsx
import React, { Component } from 'react';

class TOC extends Component {

render() {
    var lists=[];
    var data = this.props.data;
    var i = 0;
    while(i<data.length){
        lists.push(
            <li key={data[i].id}>
                <a 
                    href={"/content/" + data[i].id}
                    data-id={data[i].id}
                    onClick={function(e){
                        e.preventDefault();
                        // 
                        this.props.onChangePage(e.target.dataset.id);
                    }.bind(this)}
                >{data[i].title}</a>
            </li>);
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

- 선택 된 id를 event 객체의 target(여기서는 a 태그) - dataset에 있는 id 를 세팅해줌
- `data-` 로 시작하는 접두어를 사용하면 target의 dataset으로 접근 함

![](https://blog.kakaocdn.net/dn/C8mxB/btq1jlxmFTA/mfKKCTX0tOduqCqZc34Wb0/img.png)

- props와 state는 render 함수를 호출하기 때문에 이 값을 바꾸는 것으로 UI에 변경 사항을 적용 할 수 있음

![](https://user-images.githubusercontent.com/6733004/45537891-7d099d80-b840-11e8-986e-b64b5cf636e6.jpg)

