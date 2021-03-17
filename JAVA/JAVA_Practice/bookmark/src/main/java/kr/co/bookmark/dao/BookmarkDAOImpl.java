package kr.co.bookmark.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.BookmarkVO;

@Repository
public class BookmarkDAOImpl implements BookmarkDAO{

	@Autowired
	SqlSession sqlSession;
	

	@Override
	public void bookmarkInsert(BookmarkVO bookmarkVO) throws DataAccessException {
		sqlSession.insert("mapper.bookmark.bookmarkInsert",bookmarkVO);
	}
	

	@Override
	public List<BookmarkVO> bookmarkList() throws Exception {
		System.out.println("북마크 DAO 리스트 호출");
		return sqlSession.selectList("mapper.bookmark.selectAllBookmarkList");

	}
	

}
