## 리액트에서 특정 문자 전부 변경 및 개행 추가하기



### 리액트에서는 다음과 같이 텍스트를 뿌렸을 경우 \<br/> 태그가 안먹힌다.

```jsx
// reaplace()는 첫 문자만 바꿔주기 때문에 아래와 같이 구현하여 사용
function replaceAll(str, searchStr, replaceStr) {
  return str.split(searchStr).join(replaceStr);
}

let data = "개행\n개행2";
data = replaceAll(data, "\n","<br />");

<pre>
	{data}   
</pre>

// 출력
개행<br />개행2 
```

<br />

태그를 직접 적용하려면 dangerouslySetInnerHTML 바로 보여주는 방식을 사용해야 하는데 이는 보안상 취약하기 때문에 지양해야 한다. 

그래서 검색해보니 너도 나도 아래와 같은 방법을 추천 했다.

<br />

```jsx
// data 값이 없거나 useState에서 세팅하는 경우 실행 순서에 따라 .map함수에서 에러를 발생시키기 때문에 
// data &&을 앞에 넣어 값이 있을 때만 실행하도록 한다.
let data = "개행\n개행2";
{data && (
    <pre >
        {
            // 개행문자를 기준으로 문자를 잘라(split) 배열로 만들고 
            //배열 사이사이 <br />태그를 넣어 뿌려줘서 개행을 넣은 효과를 내준다.
            data.split("\\n").map((line) => {
                return (
                    <span>
                        {line}
                        <br />
                    </span>
                );
            })}
    </pre>
)}

//출력
개행
개행2
```

위 기능을 컴포넌트로 만들어서 사용하는 사람, 함수로 사용하는 사람, 바로 입력하는 사람 등 다양했다.

이 외에도 \&nbsp;도 \<br />와 같은 이유로 적용되지 않기 때문에 nbsp를 적용시키는 방법 역시 다양했고 nbsp를 사용하기 위해 컴포넌트화해서 올려둔 npm 라이브러리까지 있었다. 



이 외에도 공백과 개행 표현을 위한 다양한 방법 중div 태그에 style={{whiteSpacae={"pre-wrap"}} 속성을 추가해 주는 방법도 있었지만 

나는 단순하게 pre태그를 사용하고 백엔드에서 공백과 탭을 " "와 "    "로 변경해서 보내고 프론트에서는 위 방법으로 개행만 처리해주는 방법을 선택했다.

아니면 프론트엔드에서 위 예제에서의 replaceAll함수를 이용해 공백과 탭을 " "와 "    "로 변경해줘도 결과는 동일하다.

