package kr.co.bookmark;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
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
	

	
	
	// Sign in
	@RequestMapping(value = "memberlogin", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String memberlogin(HttpServletRequest request,
		HttpServletResponse response) {
	String id=request.getParameter("id");
	String pw=request.getParameter("pw");
	
	JSONObject json=new JSONObject();
	try {
		String name = memberService.memberlogin(new MemberVO(id,pw));
		MemberVO member=new MemberVO(id,pw,name);
		
		if(name!=null) {

			HttpSession session=request.getSession();
			session.setMaxInactiveInterval(3600);
			System.out.println(session.getMaxInactiveInterval());
			session.setAttribute("member", member);
			
			json.put("name", name);
			
			System.out.println();
		}else {
			json.put("msg", "로그인 실패");
		}

		
	}catch(Exception e) {
		json.put("msg", e.getMessage());
		}	
		return json.toString();
	}		
	
	
	
	// Sign out
	@RequestMapping(value = "logout", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")			
	@ResponseBody
	public String logout(HttpServletRequest request,
			HttpServletResponse response){
		
			HttpSession session=request.getSession(false);
			session.invalidate();
			return "";
	
	}
	
	
	
	
	
	// Memeber CRUD
	// Memeber Create
	@RequestMapping(value = "memberInsert", 
			method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String memberInsert(HttpServletRequest request,
			HttpServletResponse response) {
		
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		String name=request.getParameter("name");
		
		try {
			MemberVO m=new MemberVO(id,pw,name); 
			int result = memberService.idCheck(m);
			if(result != 1) {
				memberService.memberInsert(m);
				return name+"님 회원가입 되셨습니다";	
			}else {
				return "아이디가 중복입니다.";
			}
			
		}catch(Exception e) {
			return e.getMessage();
		}	
	}		
	
	
	

	// Memeber Read
	@RequestMapping(value = "/memberList", method = RequestMethod.GET)
	public ModelAndView memberList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	
			List<MemberVO> memberList = memberService.memberList();
			ModelAndView mav = new ModelAndView();
			
			HttpSession session = request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			if(member != null) {
				mav.setViewName("memberList");
				mav.addObject("memberList", memberList);
			}else {
				mav.setViewName("index");
			}
			
			return mav;
			
	}
	
	// Memeber Read	for admin
	@RequestMapping(value = "/memberList4admin", method = RequestMethod.GET)
	public ModelAndView memberList4admin( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	
			List<MemberVO> memberList = memberService.memberList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("memberList4admin");
			mav.addObject("memberList", memberList);
			
			return mav;			
			
	
	}

	// Memeber Update
	@RequestMapping(value = "/memberUpdate", method= {RequestMethod.POST}, 
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String memberUpdate( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			String name=request.getParameter("name");
			
			
			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			String getId = member.getId();
			String getPw = member.getPw();
			
			if(getPw.equals(pw) && getId.equals(id)) {
				try {
					MemberVO m = new MemberVO(id, pw, name);
					memberService.memberUpdate(m);
				
					return "정보가 수정 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else if(getId.equals("admin")){
				try {
					MemberVO m = new MemberVO(id, pw, name);
					memberService.memberUpdate(m);
				
					return "정보가 수정 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}
			}
			else {

				return "다른 사용자 계정을 수정 할 수 없습니다.";
			}
		}
	
	
	// Memeber Delete
	@RequestMapping(value = "/memberDelete", method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String memberDelete( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			String getId = member.getId();
			String getPw = member.getPw();
			
			if(getPw.equals(pw) && getId.equals(id)) {
				try {
					MemberVO m = new MemberVO(id,pw);
					memberService.memberDelete(m);
				
					return "탈퇴 완료";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else if(getId.equals("admin")){
				try {
					MemberVO m = new MemberVO(id,pw);
					memberService.memberDelete(m);
				
					return "삭제 완료";
				}catch(Exception e) {
					return e.getMessage();
				}
			}else {
				return "다른 사용자 계정을 탈퇴 할 수 없습니다.";
			}
		}
	
	
	// Bookmark Read
	@RequestMapping(value = "/bookmarkList", method= RequestMethod.GET)
	public ModelAndView bookmarkList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			List<BookmarkVO> bookmarkList = bookmarkService.bookmarkList();
			ModelAndView mav = new ModelAndView();
			
			HttpSession session = request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			if(member != null) {
			mav.setViewName("bookmarkList");
			mav.addObject("bookmarkList", bookmarkList);
			}else {
				mav.setViewName("index");
			}
			
			return mav;
	}
	
	
	

	// Bookmark Update
	@RequestMapping(value = "/bookmarkUpdate", method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String bookmarkUpdate( HttpServletRequest request,
			HttpServletResponse response) throws Exception {

			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			String getId=member.getId();
			String getPw=member.getPw();
			
			
			Long bookmark_no = Long.parseLong(request.getParameter("bookmark_no"));
			String title=request.getParameter("title");
			String url=request.getParameter("url");
			String coment=request.getParameter("coment");
			String pw=request.getParameter("pw");
			
			String id = bookmarkService.checkWriter(bookmark_no);
			if(getPw.equals(pw) && getId.equals(id)) {
				try {
					BookmarkVO b = new BookmarkVO(title, url, coment, bookmark_no);
					bookmarkService.bookmarkUpdate(b);
				
					return bookmark_no+"번 글이 수정 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else if(getId.equals("admin")){
				try {
					BookmarkVO b = new BookmarkVO(title, url, coment, bookmark_no);
					bookmarkService.bookmarkUpdate(b);
				
					return bookmark_no+"번 글이 수정 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else {
				return "작성자가 아닙니다";
			}
			
	}	
	
	// Bookmark Delete
	@RequestMapping(value = "/bookmarkDelete", method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String bookmarkDelete( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			
		
			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			
			String getPw = member.getPw();
			String getId = member.getId();
			String pw=request.getParameter("pw");
			
			Long bookmark_no = Long.parseLong(request.getParameter("bookmark_no"));
			String id = bookmarkService.checkWriter(bookmark_no);
			System.out.println("북마크 삭제" + bookmark_no);
			
			if(getPw.equals(pw) && getId.equals(id)) {
				try {
					BookmarkVO b = new BookmarkVO(bookmark_no);
					bookmarkService.bookmarkDelete(b);
				
					return bookmark_no+"번 글이 삭제 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else if(getId.equals("admin")){
				try {
					BookmarkVO b = new BookmarkVO(bookmark_no);
					bookmarkService.bookmarkDelete(b);
				
					return bookmark_no+"번 글이 삭제 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else {
				return "작성자가 아닙니다";
			}
			
	}	

	
}
