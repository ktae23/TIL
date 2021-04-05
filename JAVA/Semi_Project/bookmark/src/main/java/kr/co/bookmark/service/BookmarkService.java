package kr.co.bookmark.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.BookmarkVO;

@Repository
public interface BookmarkService {
	public List<BookmarkVO> bookmarkList() throws Exception;
	public void bookmarkInsert(BookmarkVO bookmarkVO) throws Exception;
	public void bookmarkUpdate(BookmarkVO bookmarkVO)throws Exception;
	public Long getBookmark_no()throws Exception;
	public void bookmarkDelete(BookmarkVO bookmarkVO)throws Exception;
	public String checkWriter(Long bookmark_no) throws Exception;
}
