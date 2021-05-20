package kr.co.openeg.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.co.openeg.common.CommonCode;
import kr.co.openeg.common.CommonMessage;
import kr.co.openeg.common.Crypto;
import kr.co.openeg.domain.BoardAttachVO;
import kr.co.openeg.domain.BoardVO;
import kr.co.openeg.domain.Criteria;
import kr.co.openeg.domain.PageDTO;
import kr.co.openeg.domain.RegistVO;
import kr.co.openeg.service.BoardService;
import kr.co.openeg.service.RegistService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequestMapping("/admin/*")
@AllArgsConstructor
public class AdminController implements CommonMessage, CommonCode {

	private RegistService registService;
	private BoardService boardService;

	private void setAdminSessionInfo(HttpSession session, HttpServletResponse response, RegistVO rgvo) {
		session.setAttribute(SESS_USER_ROLE, SESS_USER_ROLE_ADMIN);
		
		Cookie c = new Cookie("userRole", CommonCode.SESS_USER_ROLE_ADMIN);
		c.setMaxAge(-1);
		response.addCookie(c);	
	}
	
	private boolean isAdmin(HttpSession session, HttpServletRequest request) {
		String userRole = ""; 	
		Cookie[] cookies = request.getCookies();
		for (Cookie c : cookies) {
			if ("userRole".equals(c.getName())) {
				userRole = c.getValue();
				break;
			}
		}
		if (!CommonCode.SESS_USER_ROLE_ADMIN.equals(userRole)) return false;
		else return true;	
	}
	
	@GetMapping("/")
	public String adminHome(HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		return "redirect:/admin/boardList";
	}
	
	@GetMapping("/checkAdmin")
	public String checkAdmin(HttpSession session, HttpServletRequest request) {
		//if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		//}
		//return "redirect:/admin/boardList";
	}

	@PostMapping("/checkAdmin")
	public String doCheckAdmin(HttpSession session, HttpServletResponse response, RegistVO rgvo, RedirectAttributes rttr) {
		String salt = registService.selectSalt(rgvo.getEmail());
		String password = rgvo.getPassword();
		password = Crypto.encrypt(password, salt);
		if (StringUtils.isNotBlank(password)) {
			rgvo.setPassword(password);
		}
		RegistVO checkUserVo = registService.checkAdmin(rgvo);		
		if (checkUserVo == null) {
			HashMap<String, String> hm = new HashMap<String, String>();
			hm.put("msg", CommonMessage.MSG_CHECKUSER_FAIL);
			rttr.addFlashAttribute("result", hm);
		
			return "redirect:/admin/checkAdmin";
		}
		
		setAdminSessionInfo(session, response, checkUserVo);
		
		return "redirect:/admin/boardList";
	}
		
	@GetMapping("/register")
	public String register(HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		return "admin/register";
	}

	@GetMapping("/registGet")
	public String registGet(@RequestParam("rgno") Long rgno, @ModelAttribute("cri") Criteria cri, Model model, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		model.addAttribute("regist", registService.get(rgno));
		return "admin/registGet";
	}

	@GetMapping("/registList")
	public String registList(Criteria cri, Model model, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		model.addAttribute("list", registService.getList(cri));

		int total = registService.getTotal(cri);
		model.addAttribute("pageMaker", new PageDTO(cri, total));
		
		return "admin/registList";
	}

	@GetMapping("/boardList")
	public String list(Criteria cri, Model model, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		model.addAttribute("list", boardService.getList(cri));

		int total = boardService.getTotal(cri);
		model.addAttribute("pageMaker", new PageDTO(cri, total));
		
		return "admin/boardList";
	}
	
	@GetMapping("/boardGet")
	public String boardGet(@RequestParam("bno") Long bno, @ModelAttribute("cri") Criteria cri, Model model, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		model.addAttribute("board", boardService.get(bno));
		return "admin/boardGet";
	}

	@GetMapping("/boardModify")
	public String boardModify(@RequestParam("bno") Long bno, @ModelAttribute("cri") Criteria cri, Model model, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		model.addAttribute("board", boardService.get(bno));
		return "admin/boardModify";
	}
	
	@PostMapping("/boardModify")
	public String boardModify(BoardVO board, @ModelAttribute("cri") Criteria cri, RedirectAttributes rttr, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		HashMap<String, String> hm = new HashMap<String, String>();
		if (boardService.modify(board)) {
			hm.put("msg", MSG_UPDATE_OK);
			rttr.addFlashAttribute("result", hm);
		} else {
			hm.put("msg", MSG_UPDATE_FAIL);
			rttr.addFlashAttribute("result", hm);
		}

		rttr.addAttribute("pageNum", cri.getPageNum());
		rttr.addAttribute("amount" , cri.getAmount ());
		rttr.addAttribute("type"   , cri.getType   ());
		rttr.addAttribute("keyword", cri.getKeyword());

		return "redirect:/admin/boardList";
	}
	
	@PostMapping("/boardRemove")
	public String remove(@RequestParam("bno") Long bno, Criteria cri, RedirectAttributes rttr, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		List<BoardAttachVO> attachList = boardService.getAttachList(bno);
		HashMap<String, String> hm = new HashMap<String, String>();
		if (boardService.remove(bno)) {
			deleteFiles(attachList);
			
			hm.put("msg", MSG_DELETE_OK);
			rttr.addFlashAttribute("result", hm);
		} else {
			hm.put("msg", MSG_DELETE_FAIL);
			rttr.addFlashAttribute("result", hm);
		}
		
		rttr.addAttribute("pageNum", 1);
		rttr.addAttribute("amount" , cri.getAmount ());
		rttr.addAttribute("type"   , cri.getType   ());
		rttr.addAttribute("keyword", cri.getKeyword());
		
		return "redirect:/admin/boardList";
	}	
	
	@GetMapping("/boardRegister")
	public String boardRegister(HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}		
		return "admin/boardRegister";
	}
	
	@PostMapping("/boardRegister")
	public String boardRegister(BoardVO board, RedirectAttributes rttr, HttpSession session, HttpServletRequest request) {
		if (!isAdmin(session, request)) {
			return "admin/checkAdmin";
		}
		
		if (board.getAttachList() != null) {
			board.getAttachList().forEach(attach -> log.info(attach.toString()));
		}
		
		boardService.register(board);
		HashMap<String, String> hm = new HashMap();
		hm.put("msg", MSG_INSERT_OK);
		rttr.addFlashAttribute("result", hm);
		
		return "redirect:/admin/boardList";
	}
	
	private void deleteFiles(List<BoardAttachVO> attachList) {
		if (attachList == null || attachList.size() == 0) {
			return;
		}

		attachList.forEach(attach -> {
			try {
				Path file = Paths.get(CONT_UPLOAD_FILE_DIR + "\\" + attach.getUploadPath() + "\\" + attach.getUuid() + "_" + attach.getFileName());
				Files.deleteIfExists(file);

				if (Files.probeContentType(file).startsWith("image")) {
					Path thumbNail = Paths.get(CONT_UPLOAD_FILE_DIR + "\\" + attach.getUploadPath() + "\\s_" + attach.getUuid() + "_" + attach.getFileName());
					Files.delete(thumbNail);
				}
			} catch (Exception e) {
				log.error("delete file error" + e.getMessage());
			}
		});
	}

}
