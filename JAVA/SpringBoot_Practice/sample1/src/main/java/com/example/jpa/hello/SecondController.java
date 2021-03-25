package com.example.jpa.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecondController {

    // @ResponseBody를 쓰지 않는 방법으로 RestController(리턴이 뷰 리졸버가 아닌 스트링)를 사용
    @RequestMapping(value = "/hello-spring", method = RequestMethod.GET)
    public String helloSpring() {
        return "hello spring";
    }

    // Rest 형식으로 GET을 받는 애너테이션
    // @RequestMapping(value = "/hello-rest", method = RequestMethod.GET)
    // 위 애너테이션과 같은 기능을 하지만 아래가 더 명확 함
    @GetMapping("/hello-rest")
    public String helloRest() {
        return "hello rest";
    }

    @GetMapping("/api/hello-world")
    public String helloRestApi() {
        return "hello rest api";
    }
}