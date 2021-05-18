### Create

- create를 누르면 입력창이 보여지고, 입력한 값을 contents에 추가하는 기능 구현
- 기능을 달기 위한 태그 구현

#### Control.js

```jsx
import React, { Component } from 'react';

class Control extends Component {
      render() {
        return(
        <ul>
          <li><a href="/create" onClick={function(e){
            e.preventDefault();
            this.props.onChangeMode('create');
          }.bind(this)}>create</a></li>

          <li><a href="/update" onClick={function(e){
            e.preventDefault();
            this.props.onChangeMode('update');
          }.bind(this)}>update</a></li>

                // 링크로 걸어둘 경우 미리 삭제가 되는 상황을 막기 위해 삭제 기능은 버튼으로 구현
          <li><input type="button" value="delete" onClick={function(e){
            e.preventDefault();
            this.props.onChangeMode('delete');
          }.bind(this)}></input></li>
        </ul>
        );
      }
    }

export default Control;
```

- e.preventDefault() 함수를 통해 링크 이동을 막고 this.props.onChangeMode() 함수를 통해 mode 값을 전달 한다.

<br/>

#### App.js에 컨트롤 컴포넌트 추가

```jsx
<Control onChangeMode={function(_mode){
        this.setState({
            mode:_mode
        })
    }.bind(this)}>
</Control>
```

- 전달받은 props를 통해 State의 mode 값을 변경 할 수 있다.

<br/>

#### render() 함수에 create 모드 추가

```jsx
else if(this.state.mode === 'create'){
      _article = <CreateContent onSubmit={function(_title, _desc){
          this.max_content_id = this.max_content_id + 1;
        //   this.state.contents.push(
        //     {id:this.max_content_id, title:_title, desc:_desc}
        // );
      // push는 원본을 수정하여 추가하는 방식
      var _contents = this.state.contents.concat(
        {id:this.max_content_id, title:_title, desc:_desc}
      )
      // concat은 원본을 수정하지 않고 연결하는 방식
      this.setState({
        contents:this.state.contents
      });
    }.bind(this)}></CreateContent>
    }
```

- render함수 다른 모드들 뒤에, return함수 앞에 위치

```jsx
  constructor(props){
    super(props);
    this.max_content_id = 3;
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
```

- Constructor에 this.max_content_id 값을 넣어 최대 id 값을 지정 해 준다.
  - state에 넣지 않는 것은 보여지지 않는 부분을 뺌으로써 불필요한 rendering을 하지 않기 위함이다.

<br/>

#### 컨텐츠 출력

- 컨텐츠가 출력 되던 부분을 {_article}이란 변수로 대체하여 read가 아닌 다른 mode에서도 컨텐츠가 나오도록 수정

```jsx
// welcome과 read 모드 끝에
 _article = <ReadContent title={_tilte} desc={_desc}></ReadContent>
// control 컴포넌트 밑에
 {_article}
```

<br/>

