# Get Multiple Values of Selected Checkboxes in JAVA

 

Form태그를 이용하여 자바스크립트 없이 단순한 동작을 하는 페이지를 제작하는 중 여러 개의 체크박스 값을 보낼 일이 있었다.

 

이를 위해 자바스크립트를 이용해 배열로 보내야하나?라고 고민하던 중 HTML5부터는 name 속성이 같으면 자동으로 배열로 보낸다는걸 알게 되었다.



```html
<!DOCTYPE html>
<html lang="en" xmlns="http://www.w3.org/1999/html">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
    <form action="/multi" method="get">
        <input type="checkbox" name="checkedValue" value="1"/>
        <input type="checkbox" name="checkedValue" value="2"/>
        <input type="checkbox" name="checkedValue" value="3"/>
        <input type="checkbox" name="checkedValue" value="4"/>
        <input type="checkbox" name="checkedValue" value="5"/>
        <input type="checkbox" name="checkedValue" value="6"/>
        <input type="checkbox" name="checkedValue" value="7"/>
        <input type="submit"/>
    </form>
</body>
</html>
```



위처럼 같은 name 속성을 가진 여러 값의 체크박스가 있을 때 이를 컨트롤러로 보내면 받을 때 -- List<자료형> name속성과 같은 이름의 변수 -- 로 받으면 된다.



```java
package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class multiSelect {
    @Autowired
    multiService service;

    @GetMapping("/multi")
    public String multi(@RequestParam List<String> checkedValue){
        for (String c : checkedValue) {
            service.insert(c);
        }
        return "selectPage";
    }
}
```



