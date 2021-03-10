package web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import web.service.MemberService;
import web.vo.MemberVO;

public class MemberControllerImpl extends MultiActionController implements MemberController {
	
	private MemberService memberService;

	public void setMemberService(MemberService memberService) {
		this.memberService = memberService;
	}
	
	public ModelAndView listMembers(HttpServletRequest request, HttpServletResponse response)
	throws Exception{
		System.out.println("listMembsers...");
		List<MemberVO> membersList=memberService.listMembers();
		ModelAndView mav=new ModelAndView("listMembers");// WEB-INF/views/listMembers.jsp
		mav.addObject("membersList",membersList);
		return mav;
	}
	

}