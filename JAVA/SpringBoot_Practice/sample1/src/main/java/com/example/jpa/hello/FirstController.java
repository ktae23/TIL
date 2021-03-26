package com.example.jpa.hello;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FirstController {

    //기본적으로 GET방식 이지만 명시 할 경우 매핑을 value로, 방식을 method로 명시해 줌
    @RequestMapping(value = "/first-url", method = RequestMethod.GET)
    public void first() {
    }
    //	@RequestMapping(value="/helloworld", method= RequestMethod.GET)
    //  value 와 method가 GET인 것이 디폴트
    //  @ResponseBody 애너테이션을 사용해 뷰페이지가 아닌 문자열 리턴 되도록

    @RequestMapping("/helloworld")
    @ResponseBody
    public String helloworld() {
        return "hello world";
    }
}