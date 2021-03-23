package com.example.jpa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FirstController {

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
