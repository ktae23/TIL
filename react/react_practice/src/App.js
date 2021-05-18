import React, { Component } from 'react';
import './App.css';
import TOC from './componet/TOC';
import Subject from './componet/Subject';
import ReadContent from './componet/ReadContent';
import CreateContent from './componet/CreateContent';
import Control from './componet/Control';

class App extends Component {
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
  render(){
    var _tilte, _desc, _article = null;

    if(this.state.mode === 'welcome'){
      _tilte = this.state.welcome.title;
      _desc = this.state.welcome.desc;
      _article = <ReadContent title={_tilte} desc={_desc}></ReadContent>

    }else if(this.state.mode === 'read'){
      var i = 0;

      while(i < this.state.contents.length){
        var data = this.state.contents[i];

        if(data.id === this.state.selected_content_id){
          _tilte = data.title;
          _desc = data.desc;
          break;
        }
        i = i + 1;
      }
      _article = <ReadContent title={_tilte} desc={_desc}></ReadContent>

    } else if(this.state.mode === 'create'){
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
              selected_content_id:Number(id)
            });
          }.bind(this)}
          data={this.state.contents}>
        </TOC>

        <Control onChangeMode={function(_mode){
          this.setState({
            mode:_mode
          })
        }.bind(this)}>
        </Control>

        {_article}

      </div>
    );
  }
}

export default App;
