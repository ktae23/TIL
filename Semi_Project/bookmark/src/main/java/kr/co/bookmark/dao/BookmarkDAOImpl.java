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
		System.out.println("DAO북마크 인서트 호출");
		sqlSession.insert("mapper.bookmark.bookmarkInsert",bookmarkVO);
	}
	

	@Override
	public List<BookmarkVO> bookmarkList() throws Exception {
		System.out.println("북마크 DAO 리스트 호출");
		return sqlSession.selectList("mapper.bookmark.selectAllBookmarkList");

	}


	@Override
	public void bookmarkUpdate(BookmarkVO bookmarkVO) throws Exception {
		System.out.println("DAO 북마크 업데이트 호출");
		sqlSession.update("mapper.bookmark.updateBookmark",bookmarkVO);
		
	}
	
	public Long getBookmark_no() throws Exception{
		System.out.println("DAO 북마크 번호");
		return sqlSession.selectOne("mapper.bookmark.bookmark_no");
	}


	@Override
	public void deleteBookmark(BookmarkVO bookmarkVO) throws Exception {
		System.out.println("DAO 북마크 삭제");
		sqlSession.delete("mapper.bookmark.deleteBookmark",bookmarkVO);
		
	}



}
