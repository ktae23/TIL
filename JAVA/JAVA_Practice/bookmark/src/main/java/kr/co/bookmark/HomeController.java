package kr.co.bookmark;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kr.co.bookmark.service.BookmarkService;
import kr.co.bookmark.service.MemberService;
import kr.co.bookmark.vo.BookmarkVO;
import kr.co.bookmark.vo.MemberVO;

@Controller
public class HomeController {

	
	@Autowired 
	MemberService memberService;
	
	@Autowired 
	BookmarkService bookmarkService;
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home() {
		return "index";
	}	
	
	@RequestMapping(value = "memberInsert", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String memberInsert(HttpServletRequest request,
			HttpServletResponse response) {
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		String name=request.getParameter("name");
		System.out.println("memberInsert:"+id+"\t"+pw+"\t"+name);
		
		try {
			MemberVO m=new MemberVO(id,pw,name); 
			memberService.memberInsert(m);
			
			return name+"님 회원가입 되셨습니다";
		}catch(Exception e) {
			return e.getMessage();
		}	
	}		
	
	
	
	@RequestMapping(value = "/bookmarkInsert", method = RequestMethod.POST)
	@ResponseBody
	public String bookmarkInsert( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			
			String title=request.getParameter("title");
			String url=request.getParameter("url");
			String coment=request.getParameter("coment");
			String memid=request.getParameter("memid");
			System.out.println("bookmarkInsert:"+title+"\t"+url+"\t"+coment+"\t"+memid);
			
			try {
				BookmarkVO b = new BookmarkVO(title, url, coment, memid);
				bookmarkService.bookmarkInsert(b);

			
				return title+"이 작성 되었습니다";
			}catch(Exception e) {
				return e.getMessage();
			}	
				
	}	
	
	
	
	@RequestMapping(value = "/bookmarkList", method = RequestMethod.GET)
	public ModelAndView bookmarkList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
			List<BookmarkVO> bookmarkList = bookmarkService.bookmarkList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("bookmarkList");
			mav.addObject("bookmarkList", bookmarkList);
			
			return mav;
	}	

	
	
	
	
	
	
	@RequestMapping(value = "/memberList", method = RequestMethod.GET)
	public ModelAndView memberList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	
			List<MemberVO> memberList = memberService.memberList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("memberList");
			mav.addObject("memberList", memberList);
			
			return mav;	
	
	}

	


	

	@RequestMapping(value = "memberlogin", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
		@ResponseBody
		public String memberlogin(HttpServletRequest request,
			HttpServletResponse response) {
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		String name=request.getParameter("name");
		System.out.println("memberlogin:"+id+"\t"+pw+"\t"+name);
		
		try {
			MemberVO m=new MemberVO(id,pw,name); 
//			memberService.memberlogin(m);
			return name+"님 로그인 되셨습니다";
		}catch(Exception e) {
			return e.getMessage();
			}	
	}		


	
}
