package com.mulcam.ai.web.service;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mulcam.ai.web.dao.BoardDAO;
import com.mulcam.ai.web.vo.BoardVO;

@Service
public class BoardService {
	@Autowired
	BoardDAO boardDAO;

	public List<BoardVO> listArticles() {
		return boardDAO.listArticles();
	}

	public void boardWrite(Map<String, Object> articleMap) {
		boardDAO.boardWrite(articleMap);
		
	}

	public BoardVO viewArticle(int articleNO) {
		return boardDAO.viewArticle(articleNO);
	}
}
