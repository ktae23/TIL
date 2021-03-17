package kr.co.bookmark.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.co.bookmark.dao.BookmarkDAO;
import kr.co.bookmark.vo.BookmarkVO;

@Service
public class BookmarkServiceImpl implements BookmarkService{
	@Autowired
	BookmarkDAO bookmarkDAO;
	

	@Override
	public List<BookmarkVO> bookmarkList() throws Exception {
		System.out.println("서비스 북마크 리스트 호출");
		return bookmarkDAO.bookmarkList();
        
	}
	
	@Override
	public void bookmarkInsert(BookmarkVO bookmarkVO) throws Exception {
		bookmarkDAO.bookmarkInsert(bookmarkVO);
	}
	
}
