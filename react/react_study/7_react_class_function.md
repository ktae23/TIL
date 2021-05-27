### Class vs Function

#### Class

- React 기능을 최대한 사용 가능
- Class 문법을 알아야 함

<br/>

```jsx
class ClassComp extends React.Component{
  state = {
    number:this.props.initNumber
  }
  render(){
    return(
      <div className="container">
        <h2>class style component</h2>
        <p>Number : {this.state.number}</p>
        <input type="button" value="random" onClick={
          function(){
            this.setState({number:Math.random()})
          }.bind(this)
        }></input>
      </div>
    )
  }
}
```

<br/>

- state를 선언 한 뒤 가져가서 사용
- bind로 묶어 줘야 함

<br/>

```jsx
class ClassComp extends React.Component{
  state = {
    number:this.props.initNumber,
    date:(new Date()).toString()
  }
  render(){
    return(
      <div className="container">
        <h2>class style component</h2>
        <p>Number : {this.state.number}</p>
        <p>Date : {this.state.date}</p>
        <input type="button" value="random" onClick={
          function(){
            this.setState({number:Math.random()})
          }.bind(this)
        }></input>
        <input type="button" value="date" onClick={
          function(){
            this.setState({date:(new Date()).toString()})
          }.bind(this)
        }></input>
      </div>
    )
  }
}
```



<br/>

#### Function

- 함수의 문법만 알면 사용 가능
- 기능이 부족
- component 내에 state 사용 불가
- life cycle API 사용 불가
- hook을 이용하면 내부적으로 state와 life cycle 활용 가능
  - react 16.8 버전에 새로 추가 됨
  - 내장 hook 또는 사용자 정의 hook 이용 가능
  - use~로 시작하는 hook은 내장 된 것

<br/>

```jsx
function FuncComp(props){
  var numberState = useState(props.initNumber);
  var number = numberState[0];
  var setNumber =  numberState[1];
  return (
    <div className="container">
      <h2>function style component</h2>
      <p>Number : {number}</p>
      <input type="button" value="random" onClick={
          function(){
            setNumber(Math.random());
          }
        }></input>
    </div>
  )
}
```

- useState()는 배열로 이루어 짐
  - 첫 번째 인자가 배열의 첫번째 자리에 들어가 초기 값이 됨
  - 배열의 두 번째 자리에는 첫번째 인자값을 변경시키는 함수가 들어가 있음
    - 따라서 배열의 두번째 값을 넣은 변수에 원하는 값을 넣어주면 해당 값으로 상태가 변경 됨

```jsx
unction FuncComp(props){
  var numberState = useState(props.initNumber);
  var number = numberState[0];
  var setNumber =  numberState[1];

  // var dateState = useState((new Date()).toString());
  // var _date = dateState[0];
  // var setDate =  dateState[1];

    //위와 동일한 코드
var [_date, setDate] = useState((new Date()).toString());

  return (
    <div className="container">
      <h2>function style component</h2>
      <p>Number : {number}</p>
      <p>Date : {_date}</p>
      <input type="button" value="random" onClick={
          function(){
            setNumber(Math.random());
          }
        }></input>
         <input type="button" value="date" onClick={
          function(){
            setDate((new Date).toString());
          }
        }></input>
    </div>
  )
}
```

- useState()는 배열이기 때문에 배열식으로 작성해도 동일

<br/>

