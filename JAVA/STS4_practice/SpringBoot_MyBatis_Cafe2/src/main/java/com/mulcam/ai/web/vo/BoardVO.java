package com.mulcam.ai.web.vo;

import java.util.Date;

public class BoardVO {
	private int level,articleNO,parentNO;
	private String title,content,imageFileName,id;
	Date writeDate;
	
	
	
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public int getArticleNO() {
		return articleNO;
	}
	public void setArticleNO(int articleNO) {
		this.articleNO = articleNO;
	}
	public int getParentNO() {
		return parentNO;
	}
	public void setParentNO(int parentNO) {
		this.parentNO = parentNO;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getImageFileName() {
		return imageFileName;
	}
	public void setImageFileName(String imageFileName) {
		this.imageFileName = imageFileName;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public Date getWriteDate() {
		return writeDate;
	}
	public void setWriteDate(Date writeDate) {
		this.writeDate = writeDate;
	}
	@Override
	public String toString() {
		return "ArticleVO [level=" + level + ", articleNO=" + articleNO + ", parentNO=" + parentNO + ", title=" + title
				+ ", content=" + content + ", imageFileName=" + imageFileName + ", id=" + id + ", writeDate="
				+ writeDate + "]";
	}

}
