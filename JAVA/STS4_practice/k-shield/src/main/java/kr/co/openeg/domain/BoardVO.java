package kr.co.openeg.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class BoardVO {
	private Long bno;
	private String title;
	private String content;
	private String writer;
	private Date regDate;
	private Date updateDate;

	private int replyCnt;

	private List<BoardAttachVO> attachList = new ArrayList<BoardAttachVO>();
}
