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
	

	
	
	// 로그인
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
	
	
	
	// 로그아웃
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
	
	
	
	
	
	// member CRUD
	// Create
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
			memberService.memberInsert(m);
			
			return name+"님 회원가입 되셨습니다";
		}catch(Exception e) {
			return e.getMessage();
		}	
	}		
	

	// Read
	@RequestMapping(value = "/memberList", method = RequestMethod.GET)
	public ModelAndView memberList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	
			List<MemberVO> memberList = memberService.memberList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("memberList");
			mav.addObject("memberList", memberList);
			
			return mav;
	}
	
	// Read	for admin
	@RequestMapping(value = "/memberList4admin", method = RequestMethod.GET)
	public ModelAndView memberList4admin( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
	
			List<MemberVO> memberList = memberService.memberList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("memberList4admin");
			mav.addObject("memberList", memberList);
			
			return mav;			
			
	
	}

	// Update
		@RequestMapping(value = "/memberUpdate", method= {RequestMethod.POST}, 
				produces = "application/text; charset=utf8")
		@ResponseBody
		public String memberUpdate( HttpServletRequest request,
				HttpServletResponse response) throws Exception {
				
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");
				String name=request.getParameter("name");
				
				try {
					MemberVO m = new MemberVO(id, pw, name);
					memberService.memberUpdate(m);
				
					return name+"님의 정보가 수정 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
		}	
		
		// Delete
		@RequestMapping(value = "/memberDelete", method= {RequestMethod.POST},
				produces = "application/text; charset=utf8")
		@ResponseBody
		public String memberDelete( HttpServletRequest request,
				HttpServletResponse response) throws Exception {
				
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");

				try {
					MemberVO m = new MemberVO(id,pw);
					memberService.memberDelete(m);
				
					return id+" 계정이 삭제 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
		}	
	
	
	
	
	
	
	
	// bookmark CRUD
	// Create
	@RequestMapping(value = "/bookmarkInsert",method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String bookmarkInsert( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
				HttpSession session=request.getSession(false);
				MemberVO member = (MemberVO) session.getAttribute("member");
				String memid=member.getId();
				
				String title=request.getParameter("title");
				String url=request.getParameter("url");
				String coment=request.getParameter("coment");
				
			
			try {
				Long bookmark_no = bookmarkService.getBookmark_no();
				System.out.println("홈컨트롤러 북마크 번호");
				BookmarkVO b = new BookmarkVO(title, url, coment, memid,bookmark_no);
				bookmarkService.bookmarkInsert(b);
				System.out.println("홈컨트롤러 북마크 인서트");
			
				return title+"이(가) 작성 되었습니다";
			}catch(Exception e) {
				return e.getMessage();
			}	
				
	}	
	
	
	// Read
	@RequestMapping(value = "/bookmarkList", method= RequestMethod.GET)
	public ModelAndView bookmarkList( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
			List<BookmarkVO> bookmarkList = bookmarkService.bookmarkList();
			ModelAndView mav = new ModelAndView();
			mav.setViewName("bookmarkList");
			mav.addObject("bookmarkList", bookmarkList);
			
			return mav;
	}	

	// Update
	@RequestMapping(value = "/bookmarkUpdate", method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String bookmarkUpdate( HttpServletRequest request,
			HttpServletResponse response) throws Exception {

			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			String pw=member.getPw();
			
			
			Long bookmark_no = Long.parseLong(request.getParameter("bookmark_no"));
			String title=request.getParameter("title");
			String url=request.getParameter("url");
			String coment=request.getParameter("coment");
			String getPw=request.getParameter("pw");
			
			
			if(getPw.equals(pw)) {
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
	
	// Delete
	@RequestMapping(value = "/bookmarkDelete", method= {RequestMethod.POST},
			produces = "application/text; charset=utf8")
	@ResponseBody
	public String bookmarkDelete( HttpServletRequest request,
			HttpServletResponse response) throws Exception {
			
		
			HttpSession session=request.getSession(false);
			MemberVO member = (MemberVO) session.getAttribute("member");
			
			String getPw=member.getPw();
			String pw=request.getParameter("pw");
			Long bookmark_no = Long.parseLong(request.getParameter("bookmark_no"));
			System.out.println("북마크 삭제" + bookmark_no);
			
			if(getPw.equals(pw)) {
				try {
					BookmarkVO b = new BookmarkVO(bookmark_no);
					bookmarkService.bookmarkDelete(b);
				
					return bookmark_no+"번 글이 삭제 되었습니다";
				}catch(Exception e) {
					return e.getMessage();
				}	
			}else {
				return "비밀번호가 다릅니다";
			}
			
	}	

	
}
