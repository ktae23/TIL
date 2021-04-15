package com.mulcam.ai.web.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mulcam.ai.web.vo.BoardVO;

@Repository
public class BoardDAO {
	
	@Autowired
	SqlSession sqlSession;

	public List<BoardVO> listArticles() {
				return sqlSession.selectList("mapper.board.selectAllBoard");
	}

	public void boardWrite(Map<String, Object> articleMap) {
		sqlSession.insert("mapper.board.insert", articleMap);
		
	}


	public BoardVO viewArticle(int articleNO){
		return sqlSession.selectOne("mapper.board.selectArticle", articleNO);
	}

}
