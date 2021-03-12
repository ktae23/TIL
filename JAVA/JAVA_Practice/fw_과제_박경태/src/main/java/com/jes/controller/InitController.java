package com.jes.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;


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
		model.addAttribute("message","벌써 수업 시작한지도 두달이나 지나고 있네요.");
		return "test1";
	}


}
