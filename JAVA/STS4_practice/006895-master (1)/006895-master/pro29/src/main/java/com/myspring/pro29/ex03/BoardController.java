package com.myspring.pro29.ex03;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/boards")
public class BoardController {
	static Logger logger = LoggerFactory.getLogger(BoardController.class);
	
	@RequestMapping(value = "/all", method = RequestMethod.GET)
	public ResponseEntity<List<ArticleVO>> listArticles() {
		logger.info("listArticles �޼��� ȣ��");
		List<ArticleVO> list = new ArrayList<ArticleVO>();
		for (int i = 0; i < 10; i++) {
			ArticleVO vo = new ArticleVO();
			vo.setArticleNO(i);
			vo.setWriter("�̼���"+i);
			vo.setTitle("�ȳ��ϼ���"+i);
			vo.setContent("�� ��ǰ�� �Ұ��մϴ�."+i);
			list.add(vo);
		}
		
		return new ResponseEntity(list,HttpStatus.OK);
	}
	
	@RequestMapping(value = "/{articleNO}", method = RequestMethod.GET)
	public ResponseEntity<ArticleVO> findArticle (@PathVariable("articleNO") Integer articleNO) {
		logger.info("findArticle �޼��� ȣ��");
		ArticleVO vo = new ArticleVO();
		vo.setArticleNO(articleNO);
		vo.setWriter("ȫ�浿");
		vo.setTitle("�ȳ��ϼ���");
		vo.setContent("ȫ�浿 ���Դϴ�");
		return new ResponseEntity(vo,HttpStatus.OK);
	}	
	
	
	@RequestMapping(value = "", method = RequestMethod.POST)
	public ResponseEntity<String> addArticle (@RequestBody ArticleVO articleVO) {
		ResponseEntity<String>  resEntity = null;
		try {
			logger.info("addArticle �޼��� ȣ��");
			logger.info(articleVO.toString());
			resEntity =new ResponseEntity("ADD_SUCCEEDED",HttpStatus.OK);
		}catch(Exception e) {
			resEntity = new ResponseEntity(e.getMessage(),HttpStatus.BAD_REQUEST);
		}
		
		return resEntity;
	}	
	
	//�����ϱ�
	@RequestMapping(value = "/{articleNO}", method = RequestMethod.PUT)
	public ResponseEntity<String> modArticle (@PathVariable("articleNO") Integer articleNO, @RequestBody ArticleVO articleVO) {
		ResponseEntity<String>  resEntity = null;
		try {
			logger.info("modArticle �޼��� ȣ��");
			logger.info(articleVO.toString());
			resEntity =new ResponseEntity("MOD_SUCCEEDED",HttpStatus.OK);
		}catch(Exception e) {
			resEntity = new ResponseEntity(e.getMessage(),HttpStatus.BAD_REQUEST);
		}
		
		return resEntity;
	}
	
	//�����ϱ�
	@RequestMapping(value = "/{articleNO}", method = RequestMethod.DELETE)
	public ResponseEntity<String> removeArticle (@PathVariable("articleNO") Integer articleNO) {
		ResponseEntity<String>  resEntity = null;
		try {
			logger.info("removeArticle �޼��� ȣ��");
			logger.info(articleNO.toString());
			resEntity =new ResponseEntity("REMOVE_SUCCEEDED",HttpStatus.OK);
		}catch(Exception e) {
			resEntity = new ResponseEntity(e.getMessage(),HttpStatus.BAD_REQUEST);
		}
		
		return resEntity;
	}	

}
