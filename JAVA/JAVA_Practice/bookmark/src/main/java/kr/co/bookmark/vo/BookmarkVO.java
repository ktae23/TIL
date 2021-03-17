package kr.co.bookmark.vo;

import java.util.Date;

import kr.co.bookmark.util.MyException;

public class BookmarkVO {
	private String title;
	private String coment;
	private String url;
	private String memid;
	private Date date;
	
	

	@Override
	public String toString() {
		return "BookmarkVO [title=" + title + ", coment=" + coment + ", url=" + url + ", memid=" + memid + ", date="
				+ date + "]";
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public BookmarkVO() {
		super();
	}
	
	public BookmarkVO(String title, String url, String coment,  String memid) throws MyException {
		setTitle(title);
		setUrl(url);
		setComent(coment);
		setMemid(memid);
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
