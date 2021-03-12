package my.jes.web3;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import my.jes.web3.service.MemberService;


@Controller
public class HomeController {
	
	@Autowired
	MemberService memberService;
	
	@RequestMapping(value={"/test/loginForm.do","/test/loginForm2.do"},
			method= {RequestMethod.GET})
	public ModelAndView loginForm(HttpServletRequest request,
			HttpServletResponse response)throws Exception{
		ModelAndView mav=new ModelAndView("loginForm");
		return mav;
	}
	
	@RequestMapping(value={"/test/login.do"},
			method= {RequestMethod.POST})
	public ModelAndView login(HttpServletRequest request,
			HttpServletResponse response)throws Exception{
		request.setCharacterEncoding("UTF-8");
		String id=request.getParameter("id");
		String pw=request.getParameter("pw");
		
		String name=memberService.login(id,pw);		
		
		ModelAndView mav=new ModelAndView("result");
		mav.addObject("name",name);
		return mav;
	}
	

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model) {		
				
		return "home";
	}
	
}
