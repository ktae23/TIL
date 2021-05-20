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
import kr.co.openeg.domain.AnswerAttachVO;
import kr.co.openeg.domain.AnswerVO;
import kr.co.openeg.service.AnswerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/answer/*")
@AllArgsConstructor
public class AnswerController implements CommonMessage, CommonCode {

	private static final String SESS_ANSWER_NO = "ANSWER_NO";
	private AnswerService service;

	@GetMapping("/answer")
	public String answer(HttpSession session) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		Long asno = service.getAsno(rgno);
		if (asno != null && asno > 0) {
			session.setAttribute(SESS_ANSWER_NO, asno);
			return "redirect:/answer/get";
		} else {
			/*
			 * TODO CSRF Secure Coding
			 * 		1) 임의의 난수를 생성
			 * 		2) csrf_s_token 이름으로 세션에 저장
			 * 		3) 뷰(/answer/answer)로 전달 
			 */
			return "";
		}
	}

	@PostMapping("/answer")
	public String answer(HttpSession session, RedirectAttributes rttr, AnswerVO asvo) {

		HashMap<String, String> hm = new HashMap<String, String>();
		
		/*
		 * TODO CSRF Secure Coding
		 * 		1) 세션에 저장된 csrf_s_token 값을 추출
		 * 		2) 요청 파라미터로 전달된 csrf_p_token 값을 추출
		 * 		3) 두 값을 비교하여 일치하지 않는 경우 정답제출(/answer/answer)페이지로 리다이렉트 
		 */
		
		
		
		
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		asvo.setRgno(rgno);
		service.answer(asvo);
		
		session.setAttribute(SESS_ANSWER_NO, asvo.getAsno());
		
		hm.put("msg", CommonMessage.MSG_INSERT_OK);
		rttr.addFlashAttribute("result", hm);
		
		return "redirect:/answer/get";				
	}
	
	@RequestMapping("/get")
	public String get(HttpSession session, Model model, AnswerVO asvo) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		Long asno = (Long)session.getAttribute(SESS_ANSWER_NO);
		if (asno == null || rgno == null) {
			return "redirect:/";
		} else {
			asvo.setRgno(rgno);
			asvo.setAsno(asno);
			
			model.addAttribute("answer", service.get(asvo));
			return "answer/get";
		}
	}

	@GetMapping("/modify")
	public String modify(HttpSession session, Model model, AnswerVO asvo) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		Long asno = (Long)session.getAttribute(SESS_ANSWER_NO);
		if (asno == null || rgno == null) {
			return "redirect:/";
		} else {
			asvo.setRgno(rgno);
			asvo.setAsno(asno);
			
			model.addAttribute("answer", service.get(asvo));
			return "answer/modify";
		}
	}

	@PostMapping("/modify")
	public String modify(HttpSession session, AnswerVO asvo, RedirectAttributes rttr) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		Long asno = (Long)session.getAttribute(SESS_ANSWER_NO);
		if (asno == null || rgno == null) {
			return "redirect:/";
		} 
		
		asvo.setRgno(rgno);
		asvo.setAsno(asno);
		
		HashMap<String, String> hm = new HashMap<String, String>();
		if (service.modify(asvo)) {
			hm.put("msg", MSG_UPDATE_OK);
		} else {
			hm.put("msg", MSG_UPDATE_FAIL);
		}
		rttr.addFlashAttribute("result", hm);
		return "redirect:/answer/get";
	}

	@PostMapping("/remove")
	public String remove(HttpSession session, AnswerVO asvo, RedirectAttributes rttr) {
		Long rgno = (Long)session.getAttribute(SESS_USER_RGNO);
		asvo.setRgno(rgno);
		
		HashMap<String, String> hm = new HashMap<String, String>();
		if (service.remove(asvo)) {
			hm.put("msg", MSG_DELETE_OK);
			hm.put("url", "/");
			rttr.addFlashAttribute("result", hm);
		} else {
			hm.put("msg", MSG_DELETE_FAIL);
			rttr.addFlashAttribute("result", hm);
		}
		return "redirect:/answer/get?asno=" + asvo.getAsno();
	}

	@GetMapping(value = "/getAttachList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public ResponseEntity<List<AnswerAttachVO>> getAttachList(Long asno, Long rgno) {
		return new ResponseEntity<>(service.getAttachList(asno, rgno), HttpStatus.OK);
	}
}
