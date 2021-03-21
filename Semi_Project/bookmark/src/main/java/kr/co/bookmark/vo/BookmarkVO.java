package kr.co.bookmark.vo;

import java.util.Date;

import kr.co.bookmark.util.MyException;

public class BookmarkVO {
	private String title;
	private String coment;
	private String url;
	private String memid;
	private Date date;
	private long bookmark_no;


	public BookmarkVO() {
		super();
	}
	public BookmarkVO(String title, String url, String coment,  String memid, Long bookmark_no) throws MyException {
		
		this(title, url, coment, memid);
		setBookmark_no(bookmark_no);
	}	
	
	
	public BookmarkVO(String title, String url, String coment,  String memid) throws MyException {
		super();
		setTitle(title);
		setUrl(url);
		setComent(coment);
		setMemid(memid);
	}	

	public BookmarkVO(String memid, Long bookmark_no) throws MyException {
		setMemid(memid);
		setBookmark_no(bookmark_no);
	}
	
	public BookmarkVO(String title, String url, String coment, Long bookmark_no) throws MyException {
		super();
		setTitle(title);
		setUrl(url);
		setComent(coment);
		setBookmark_no(bookmark_no);
	}
	
	public BookmarkVO(Long bookmark_no) throws MyException {
		setBookmark_no(bookmark_no);
		}
	@Override
	public String toString() {
		return "BookmarkVO [title=" + title + ", coment=" + coment + ", url=" + url + ", memid=" + memid + ", date="
				+ date + ", bookmark_no=" + bookmark_no + "]";
	}

	public long getBookmark_no() {
		return bookmark_no;
	}

	public void setBookmark_no(long bookmark_no) throws MyException {
		if(bookmark_no!=0) {
			this.bookmark_no = bookmark_no;
		}else {
			throw new MyException("bookmark_no가 입력되지 않았습니다");
		}
	}


	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) throws MyException {
		if(title!=null) {
			this.title = title;
		}else {
			throw new MyException("title가 입력되지 않았습니다");
		}
	}
	public String getComent() {
		return coment;
	}
	public void setComent(String coment) throws MyException {
		if(coment!=null) {
			this.coment = coment;
		}else {
			throw new MyException("coment가 입력되지 않았습니다");
		}
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) throws MyException {
		if(url!=null) {
			this.url = url;
		}else {
			throw new MyException("url이 입력되지 않았습니다");
		}
	}
	public String getMemid() {
		return memid;
	}
	public void setMemid(String memid) throws MyException {
		if(memid != null) {
			this.memid = memid;
		}else {
			throw new MyException("bookmarkmemid가 입력되지 않았습니다.");
		}
	}
	
	
		
	}
