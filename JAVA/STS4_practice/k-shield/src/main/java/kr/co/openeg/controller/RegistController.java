package kr.co.openeg.controller;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.co.openeg.common.CommonCode;
import kr.co.openeg.common.CommonMessage;
import kr.co.openeg.common.Crypto;
import kr.co.openeg.domain.Criteria;
import kr.co.openeg.domain.PageDTO;
import kr.co.openeg.domain.RegistAttachVO;
import kr.co.openeg.domain.RegistVO;
import kr.co.openeg.service.RegistService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/regist/*")
@AllArgsConstructor
public class RegistController implements CommonMessage, CommonCode {

	private RegistService service;

	private void setSessionInfo(HttpSession session, RegistVO rgvo) {
		session.setAttribute(SESS_USER_RGNO, rgvo.getRgno ());
		session.setAttribute(SESS_USER_MAIL, rgvo.getEmail());
		session.setAttribute(SESS_USER_NAME, rgvo.getName ());
		session.setAttribute(SESS_USER_ROLE, SESS_USER_ROLE_MEMBER);
	}
	
	/* LAB CSRF : Insecure Code */
	@GetMapping("/register")
	public void register() {

	}
		
	/* LAB SQL Injection : Insecure Code */
	@PostMapping("/register")
	public String register(HttpSession session, RedirectAttributes rttr, RegistVO rgvo) {

		HashMap<String, String> hm = new HashMap<String, String>();
		if (service.checkEmail(rgvo.getEmail()) != null) {
			hm.put("msg", CommonMessage.MSG_INSERT_FAIL);
			rttr.addFlashAttribute("result", hm);
			return "redirect:/regist/register";
		} else {
			service.register(rgvo);
			setSessionInfo(session, rgvo);
			hm.put("msg", CommonMessage.MSG_INSERT_OK);
			rttr.addFlashAttribute("result", hm);
			return "redirect:/regist/get";
		}		
	}
	
	@RequestMapping("/get")
	public String get(HttpSession session, Model model) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		if (rgno == null) {
			return "redirect:/";
		} else {
			model.addAttribute("regist", service.get(rgno));
			return "regist/get";
		}
	}

	@GetMapping("/modify")
	public String modify(HttpSession session, Model model) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		if (rgno == null) {
			return "redirect:/";
		} else {
			model.addAttribute("regist", service.get(rgno));
			return "regist/modify";
		}
	}

	@PostMapping("/modify")
	public String modify(HttpSession session, RegistVO rgvo, RedirectAttributes rttr) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		if (rgno == null) {
			return "redirect:/";
		} 
		
		String password = rgvo.getPassword();
		
		HashMap<String, String> hm = new HashMap<String, String>();
		RegistVO savedRgvo = service.get(rgno);
		
		if (StringUtils.isNotBlank(password) && StringUtils.equals(savedRgvo.getPassword(), password)) {
			rgvo.setPassword(password);
			if (service.modify(rgvo)) {
				hm.put("msg", MSG_UPDATE_OK);
			} else {
				hm.put("msg", MSG_UPDATE_FAIL);
			}
		} else {
			hm.put("msg", MSG_UPDATE_FAIL);
		}
		rttr.addFlashAttribute("result", hm);
		return "redirect:/regist/get";
	}

	@GetMapping("/admin/registGet")
	public void regsistGet(HttpSession session, @RequestParam("rgno") Long rgno, @ModelAttribute("cri") Criteria cri, Model model) {
		model.addAttribute("regist", service.get(rgno));
	}
	
	@GetMapping("/checkUser")
	public String checkUser(HttpSession session) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		if (rgno == null) {
			return "regist/checkUser";
		}
		return "redirect:/regist/get";
	}

	
	
	/* LAB SQL Injection : Insecure Code */
	@PostMapping("/checkUser")
	public String doCheckUser(HttpSession session, RegistVO rgvo, RedirectAttributes rttr) {
		RegistVO checkUserVo = service.checkUser(rgvo);
		if (checkUserVo == null) {
			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put("msg", CommonMessage.MSG_CHECKUSER_FAIL);
			rttr.addFlashAttribute("result", hm);
			return "redirect:/regist/checkUser";
		}
		
		setSessionInfo(session, checkUserVo);
		return "redirect:/regist/get";
	}
	

	@PostMapping("/remove")
	public String remove(RegistVO rgvo, Criteria cri, RedirectAttributes rttr) {
		HashMap<String, String> hm = new HashMap<String, String>();
		if (service.remove(rgvo)) {
			hm.put("msg", MSG_DELETE_OK);
			hm.put("url", "/");
			rttr.addFlashAttribute("result", hm);
		} else {
			hm.put("msg", MSG_DELETE_FAIL);
			rttr.addFlashAttribute("result", hm);
		}
		return "redirect:/regist/get?rgno=" + rgvo.getRgno();
	}

	@GetMapping("/admin/registList")
	public void registList(Criteria cri, Model model) {
		model.addAttribute("list", service.getList(cri));

		int total = service.getTotal(cri);
		model.addAttribute("pageMaker", new PageDTO(cri, total));
	}

	@GetMapping(value = "/getAttachList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public ResponseEntity<List<RegistAttachVO>> getAttachList(Long rgno) {
		return new ResponseEntity<>(service.getAttachList(rgno), HttpStatus.OK);
	}
}
