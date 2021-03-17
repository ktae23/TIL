package kr.co.bookmark.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.BookmarkVO;

@Repository
public interface BookmarkService {
	public List<BookmarkVO> bookmarkList() throws Exception;
	public void bookmarkInsert(BookmarkVO bookmarkVO) throws Exception;
}
