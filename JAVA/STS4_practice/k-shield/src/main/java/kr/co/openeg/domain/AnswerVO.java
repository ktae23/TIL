package kr.co.openeg.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class AnswerVO {
	private Long asno;
	private Long rgno;
	private String content;
	private Date regDate;
	private Date updateDate;
	
	private String csrf_p_token;

	private List<AnswerAttachVO> attachList = new ArrayList<AnswerAttachVO>();
}
