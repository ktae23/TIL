import React, { Component } from 'react';

class Subject extends Component {
    // function render(){} 여야 하지만 javascript의 최신 스펙이 들어간 class 내부에서는 생략
      render() {
        return(
          // 컴포넌트를 만들 때는 반드시 하나의 최상위 태그만 사용해야만 함
          <header>
            <a href="/" onClick={function(e){
              e.preventDefault();
              this.props.onChangePage();
            }.bind(this)}>
              <h1>{this.props.title}</h1></a>
            {this.props.sub}
          </header>
        );
      }
    }

export default Subject;