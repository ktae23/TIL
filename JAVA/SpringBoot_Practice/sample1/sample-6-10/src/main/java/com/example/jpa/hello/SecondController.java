package com.example.jpa.hello;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecondController {

	/**
	 3. 클라이언트 요청에 대한 주소에 대한 Rest 함수를 작성해 봅시다.
	 [조건]
	 - 컨트롤러 인식을 위한 Controller 어노테이션 이용
	 - 주소매핑은 RequestMapping에 아닌 Rest 형식의 어노테이션 이용
	 - HTTP 메소드는 GET 방식(어노테이션 이용)
	 - 요청 주소는 "/hello-spring"
	 - 리턴값은 "hello spring" 문자열 리턴
	 - 문자열을 리턴하기 위한 어노테이션 이용
	 */
	@RequestMapping(value = "/hello-spring", method = RequestMethod.GET)
	public String helloString() {

		return "hello spring";
	}

	/**
	 4. 클라이언트 요청에 대한 Rest 형식의 함수를 작성해 보세요.
	 [조건]
	 - Rest 컨트롤러 형식의 어노테이션 이용
	 - 주소매핑 역시 Rest 형식의 어노테이션 이용
	 - HTTP 메소드는 GET
	 - 요청 주소는 "/hello-rest"
	 - 리턴값은 ""hello rest"" 문자열 리턴
	 */
	@GetMapping("/hello-rest")
	public String helloRest() {
		
		return "hello rest";
	}

	/**
	 5. 클라이언트 요청에 대한 Rest API 형식의 함수를 작성해 보세요.
	 [조건]
	 - Rest 컨트롤러 형식의 어노테이션 이용
	 - 주소매핑 역시 Rest 형식의 어노테이션 이용
	 - HTTP 메소드는 GET
	 - 요청 주소는 "/api/helloworld"
	 - 리턴값은 "hello rest api" 문자열 리턴
	 */
	@GetMapping("/api/helloworld")
	public String helloRestApi() {
		
		return "hello rest api";
	}
	
	
	

}
