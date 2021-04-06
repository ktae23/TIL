package com.mulcam.ai.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mulcam.ai.web.service.MemberService;
import com.mulcam.ai.web.vo.MemberVO;

@Controller
public class MemberController {
	
	@Autowired
	MemberService memberService;
	
	@RequestMapping(value = "logout.chr", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")			
	@ResponseBody
	public String logout(HttpServletRequest request,
			HttpServletResponse response){
		
			HttpSession session=request.getSession(false);
			session.invalidate();
			return "";
	}
	
	@RequestMapping(value = "login.chr", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")			
	@ResponseBody
	public String login(HttpServletRequest request,
			HttpServletResponse response){
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");	
		
		JSONObject json=new JSONObject();
		
		try {
			MemberVO m=new MemberVO(id,pw); 
			String name=memberService.login(m);
			if(name!=null) {
				HttpSession session=request.getSession();
				session.setAttribute("member", m);
				json.put("name", name); // {"name":"최혜린"}
			}else {
				json.put("msg", "로그인 실패"); // {"msg":"로그인 실패"}
			}
		}catch(Exception e) {
			json.put("msg", e.getMessage()); // {"msg":"SQLException"}
		}
		return json.toJSONString();
	}

	
	@RequestMapping(value = "memberInsert.chr", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
			
	@ResponseBody
	public String memberInsert(HttpServletRequest request,
			HttpServletResponse response)throws Exception{
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		String name=request.getParameter("name");
		System.out.println("memberInsert:"+id+"\t"+pw+"\t"+name);
	
		try {
			MemberVO m=new MemberVO(id,pw,name); 
			memberService.memberInsert(m);
			return name+"님 회원가입 되셨습니다.";
		}catch(Exception e) {
			return e.getMessage();
		}
		
	}
	
}
