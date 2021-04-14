package com.mulcam.ai.web.controller;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.mulcam.ai.web.service.BoardService;
import com.mulcam.ai.web.vo.BoardVO;
import com.mulcam.ai.web.vo.MemberVO;

@Controller
public class BoardController {

	@Autowired
	BoardService boardService;

/** 모든 글 보기 */
	@RequestMapping(value = "boardList", method = { RequestMethod.GET }, produces = "application/text; charset=utf8")
	public ModelAndView boardList(HttpServletRequest request) {

		ModelAndView mav = new ModelAndView("board");
		List<BoardVO> articlesList = boardService.listArticles();
		System.out.println(articlesList.size());
		mav.addObject("articlesList", articlesList);
		return mav;
	}

/** 글 쓰기 화면 얻기 */
	@RequestMapping(value = "boardWriteForm", method = {
			RequestMethod.GET }, produces = "application/text; charset=utf8")
	public String boardWriteForm(HttpServletRequest request) {
		return "boardWriteForm";
	}
	
/** 로그인  화면 얻기 */
	@RequestMapping(value = "loginForm", method = {
			RequestMethod.GET }, produces = "application/text; charset=utf8")
	public String loginForm(HttpServletRequest request) {
		return "loginForm";
	}

	
/** 글 쓰기 처리 */
           @RequestMapping(value = "boardWrite", method = { RequestMethod.POST }, produces = "application/text; charset=utf8")
	public RedirectView boardWrite(MultipartHttpServletRequest multipartRequest) {
		try {
			multipartRequest.setCharacterEncoding("utf-8");
			Map<String, Object> articleMap = new HashMap<String, Object>();
			Enumeration<String> enu = multipartRequest.getParameterNames();

			while (enu.hasMoreElements()) {
				String name = enu.nextElement();
				String value = multipartRequest.getParameter(name);
				articleMap.put(name, value);
			}
			MultipartFile file = multipartRequest.getFile("file");
			if (file != null) {
				String fileName = file.getOriginalFilename();
				if (!fileName.equals("")) {
					System.out.println(fileName);
					file.transferTo(new File("d:\\tool\\temp\\" + fileName));
					articleMap.put("imageFileName", fileName);
				} else {
					articleMap.put("imageFileName", "");
				}
			} else {
				articleMap.put("imageFileName", "");
			}
			HttpSession session = multipartRequest.getSession();
			MemberVO memberVO=null;
			if(session==null || (memberVO = (MemberVO) session.getAttribute("member"))==null) {
				return new RedirectView("loginForm");
			}
			
			String id = memberVO.getId();
System.out.println("parentNO:"+articleMap.get("parentNO"));
			if(articleMap.get("parentNO")==null) {
				articleMap.put("parentNO", 0);
			}
			articleMap.put("id", id);

			boardService.boardWrite(articleMap);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new RedirectView("boardList");
	}


/**  글 내용 보기 */
	@RequestMapping(value = "viewArticle", method = RequestMethod.GET)
	public ModelAndView viewArticle(@RequestParam("articleNO") int articleNO, HttpServletRequest request,
			HttpServletResponse response) throws Exception {	
		System.out.println(articleNO+"번 글 보기");
		BoardVO boardVO = boardService.viewArticle(articleNO);
		ModelAndView mav = new ModelAndView();
		mav.setViewName("viewArticle");
		mav.addObject("article", boardVO);
		return mav;
	}
	
/**  답글 작성 화면 얻기 */
	@RequestMapping(value = "replyForm", method =  RequestMethod.POST)
	private ModelAndView form(HttpServletRequest request, HttpServletResponse response) throws Exception {		
		ModelAndView mav = new ModelAndView();
		mav.addObject("parentNO", request.getParameter("parentNO"));
		mav.setViewName("replyForm");
		return mav;
	}

/** 파일 업로드하기 */
	private String upload(MultipartHttpServletRequest multipartRequest) throws Exception{
		String imageFileName= null;
		Iterator<String> fileNames = multipartRequest.getFileNames();
		
		while(fileNames.hasNext()){
			String fileName = fileNames.next();
			MultipartFile mFile = multipartRequest.getFile(fileName);
			imageFileName=mFile.getOriginalFilename();
			File file = new File("d:\\tool\\temp\\"+ fileName);
			if(mFile.getSize()!=0){ //File Null Check
				if(! file.exists()){ //경로상에 파일이 존재하지 않을 경우
					if(file.getParentFile().mkdirs()){ //경로에 해당하는 디렉토리들을 생성
							file.createNewFile(); //이후 파일 생성
					}
				}
				mFile.transferTo(new File("d:\\tool\\temp\\"+imageFileName)); //임시로 저장된 multipartFile을 실제 파일로 전송
			}
		}
		return imageFileName;
	}
}
