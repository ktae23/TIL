package kr.co.openeg.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class CommonController {
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home() {
		return "home";
	}
	
	@RequestMapping(value = "/eventOverview", method = RequestMethod.GET)
	public String intro() {
		return "eventOverview";
	}
	
	@GetMapping("/logout")
	public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		
		Cookie[] cookies = request.getCookies();
		for (Cookie c : cookies) {
			c.setMaxAge(0);
			response.addCookie(c);
		}
		
		session.invalidate();
		
		return "redirect:/";
	}
	
	
	/* LAB Command Injection : Insecure Code */
	@GetMapping(value = "/getDateTimeAction")
	@ResponseBody
	public ResponseEntity<String> getDateTimeAction(String cmd) {
		StringBuffer now = new StringBuffer();
		String[] cmds = new String[] { "cmd.exe", "/c", cmd }; 
		Process process = null;
		InputStream is = null;
		Scanner scanner = null;
		try {
			process = Runtime.getRuntime().exec(cmds);
			is = process.getInputStream();
			scanner = new Scanner(is);		
			while (scanner.hasNext()) {
				now.append(scanner.next());
			}
		} catch (IOException ioe) {
			log.error(ioe.getMessage());
		} finally {
			if (scanner != null) {
				try { scanner.close(); } catch (Exception e) { log.error(e.getMessage()); }
			}
			if (is != null) {
				try { is.close(); } catch (IOException e) { log.error(e.getMessage()); }
			}
		}
		return new ResponseEntity<String>(now.toString(), HttpStatus.OK);
	}

}
