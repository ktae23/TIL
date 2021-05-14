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
        contents:[
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
