How to require at least one checkbox be checked before a form can be submitted without Ajax

How to implement "select all" check box in HTML

How to use String methodes like equeals, contains in Thymeleaf

How to send diffrent value when checked select All check box

How to Save Multiple Value Of Check Box List In Database?

How to set check box to check based on database value



**요구 사항 :** 

1. 사용자 정보를 볼 때 사용자가 보유한 기술을 DB에서 받아와 사전 체크 해둠

2. 전체 선택 및 전체 선택 해제

3. 전체 선택 시 값으로 '*'가 전송 됨

4. 여러개 선택 시 "1,2" 와 같이 여러 값이 콤마로 구분되어 DB에 입력 됨

5. AJAX를 사용하지 않고 FORM 태그를 사용

6. 최소한 한 개 이상의 값을 선택



**HTML(Thymeleaf)**

```html
<form action="/modify" method="post" id="formId">

    <input type="checkbox" th:checked="${#strings.equals(user.skill,'All')}"
                           onclick="selectAll(this)"/> <b>전체 선택</b>

    <input type="checkbox" name="checkbox" value="1"
    th:checked="${#strings.contains(user.skill,'Python')}">Python</input>

    <input type="checkbox" name="checkbox" value="2"
    th:checked="${#strings.contains(user.skill,'Java')}">Java</input>

    <input type="checkbox" name="checkbox" value="3"
    th:checked="${#strings.contains(user.skill,'JS')}">JS</input>

    <input type="hidden" id="SA"/>

</form>
```

[참고 블로그](https://cizz3007.github.io/%ED%83%80%EC%9E%84%EB%A6%AC%ED%94%84/syntax/thymeleaf/2018/04/10/thymeleaf2/)

[타임리프 공식 문서](https://www.thymeleaf.org/doc/tutorials/2.1/usingthymeleaf.html#strings)

${strings.스트링메서드(적용 대상1, 적용 대상2)} 



**Javascript**

```javascript
function selectAll(selectAll) {
    const checkboxes
    = document.getElementsByName("checkbox");
    let SA = document.getElementById("SA");

    if (selectAll.checked) {
        checkboxes.forEach((checkbox) => {
            checkbox.checked = selectAll.checked;
            checkbox.setAttribute("disabled", "disabled");
        })
        SA.setAttribute("value", "*");
        SA.setAttribute("name", "checkbox");
    } else {
        checkboxes.forEach((checkbox) => {
            checkbox.checked = selectAll.checked;
            checkbox.removeAttribute("disabled");
        })
        SA.removeAttribute("value");
        SA.removeAttribute("name");
    }
}
```

전체선택을 누르면 나머지 체크박스를 모두 disabled처리해서 값이 날아가지 못하도록 처리하고

별도 값을 전송하기 위해 마련해둔 <input type="hidden" id="SA"/>에 값과 이름을 추가해 준다



이렇게하면 폼 전송을 했을 때 모든 값이 전송 되는 것이 아니라 별도로 설정해 둔 기본 값 하나만 전송 된다.



```javascript
$(document).ready(function () {

    $("#formId").submit(function () {
        var checked = $("#formId input:checked").length > 0;
        if (!checked) {
            alert("최소한 하나 이상 선택하세요.");
            return false;
        }
    });
})
```

Ajax를 쓰면야 검증이 쉽겠지만 다른 값들도 같이 넘기기 때문에 Form의 submit을 그대로 사용하는 수준에서 검증을 해야 했다.



폼 안의 checked 값이 하나도 없다면 false를 반환하여 submit을 막는다.



**Java**

```java
    /**
     * @param userId
     * @param skill
     * @return
     */
    @PostMapping("modify")
    public String confirmUser(@RequestParam String userId, @RequestParam List<String> skill) {

        String skillData;
        if(skill.size() > 1) {
            skillData = StringUtils.join(skill, ",");
        }else{
            skillData = skill.get(0);
        }
        result = service.modify(userId, skillData);

    }
```

전체선택을 해서 하나의 값만 넘어오더라도 파라미터는 리스트로 설정되어 있기 때문에 리스트로 받아진다.



이때 리스트의 길이가 2 이상이면 콤마를 구분자로 문자열로 묶고, 값이 하나만 넘어 왔을 경우 그 값을 꺼내 바로 저장한다.