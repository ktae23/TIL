package kr.co.bookmark.dao;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.BookmarkVO;

@Repository
public interface BookmarkDAO {

	public void bookmarkInsert(BookmarkVO bookmarkVO)throws Exception;
	public List<BookmarkVO> bookmarkList()throws Exception;
	public void bookmarkUpdate(BookmarkVO bookmarkVO)throws Exception;
	public Long getBookmark_no() throws Exception;
	public void deleteBookmark(BookmarkVO bookmarkVO)throws Exception;
}
