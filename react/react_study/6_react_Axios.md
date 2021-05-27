```javascript
axios.post('/url/path', null, {params : {
                  id : _id,
                  pw : _pw
              }})
                .then((Response)=>{       	    		             
                  window.$('#result').append(Response.data); 
              })
                .catch(() => {   
                  window.$('#result').append("요청처리 실패"); 
              });
            }}
```

- 원래는 첫 번째 인자가 url, 두 번째 인자가 data, 세 번째 인자가 config여야 하나,  jquery 사용 시 잘 안되는 것 같다. 
- post 메소드의 두 번째 값을 null로 넣고 세번 째 인자에 params을 넣어주면 전송이 된다.

```javascript
axios.get('/url/path')
                .then((Response)=>{       	    		             
                  window.$('#result').append(Response.data); 
              })
                .catch(() => {   
                  window.$('#result').append("요청처리 실패"); 
              });
            }}
```

- get 메소드는 요청만해서 값을 받아오는 용도로 사용하기 좋다.

```javascript
axios({
    url : '/url/path',
    method : 'post',
    data:{
        data:data
    }
	})  
    	.then((Response)=>{       	    		             
        	window.$('#result').append(Response.data); 
    })
        .catch(() => {   
        	window.$('#result').append("요청처리 실패"); 
    });
    }}
```

- ajax처럼 이렇게도 사용 가능

