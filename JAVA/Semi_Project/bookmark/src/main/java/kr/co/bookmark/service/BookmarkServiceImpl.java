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
		System.out.println("서비스 인서트 호출");
		bookmarkDAO.bookmarkInsert(bookmarkVO);
	}
	

	@Override
	public void bookmarkUpdate(BookmarkVO bookmarkVO) throws Exception {
		System.out.println("서비스 북마크 업데이트 호출");
		bookmarkDAO.bookmarkUpdate(bookmarkVO);
	}

	@Override
	public Long getBookmark_no() throws Exception {
		System.out.println("서비스 북마크 번호");
		return bookmarkDAO.getBookmark_no();
	}

	@Override
	public void bookmarkDelete(BookmarkVO bookmarkVO) throws Exception {
		System.out.println("서비스 북마크 삭제");
		bookmarkDAO.deleteBookmark(bookmarkVO);
	}
	
	@Override
	public String checkWriter(Long bookmark_no) throws Exception {
		System.out.println("서비스 작성자 체크 호출");
		return bookmarkDAO.checkWriter(bookmark_no);
	}

	
}
