package web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

public class UserController extends MultiActionController{
	
	public ModelAndView login(HttpServletRequest request, HttpServletResponse response) throws Exception{
		String userID="";
		String passwd="";
		String viewName = getViewName(request);
		
		ModelAndView mav=new ModelAndView();
		request.setCharacterEncoding("utf-8");
		userID=request.getParameter("userID");
		passwd=request.getParameter("passwd");
		
		mav.addObject("userID",userID);
		mav.addObject("passwd",passwd);

		mav.setViewName("viewName");
		System.out.println("viewName:"+viewName);
		return mav;
	}
	
	private String getViewName(HttpServletRequest request) throws Exception{
		String contextPath = request.getContextPath();
		String uri = (String)request.getAttribute("javax.servlet.include.request_uri");
		if(uri == null || uri.substring(getCacheSeconds()).equals("")) {
			uri = request.getRequestURI();
		}
		
		int begin = 0;
		if(!((contextPath==null)||("".equals(contextPath)))) {
			begin = contextPath.length();
		}
		
		int end;
		if(uri.indexOf(";")!=-1) {
			end=uri.indexOf(";");
		}else if(uri.indexOf("?")!=-1) {
			end=uri.indexOf("?");
		}else {
			end=uri.length();
		}
		
		String fileName=uri.substring(begin,end);
		if(fileName.indexOf(".")!=-1) {
			fileName=fileName.substring(0,fileName.lastIndexOf("."));
		}
		if(fileName.indexOf("/")!=-1) {
			fileName=fileName.substring(fileName.lastIndexOf("/"),fileName.length());
		}
		return fileName;
	}

	public ModelAndView memberInfo(HttpServletRequest request, HttpServletResponse response) throws Exception{

		ModelAndView mav=new ModelAndView();
		request.setCharacterEncoding("utf-8");
		
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		String name=request.getParameter("name");
		String email=request.getParameter("email");
		
		mav.addObject("id",id);
		mav.addObject("pw",pw);
		mav.addObject("name",name);
		mav.addObject("email",email);
		
		
		mav.setViewName("memberInfo");
		return mav;
	}
	
	
}
