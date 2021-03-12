package com.jes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@Controller
public class InitController {
//	@ResponseBody
//	@RequestMapping("/")
//	public String home(){
//		System.out.println("index");
//		return "index";
//	}
//	
	@RequestMapping("/")
	public String hello(Model model){
		System.out.println("hello");
		model.addAttribute("message","주목받는 AI 9대 핵심 기술 분석");
		return "test1";
	}


}
