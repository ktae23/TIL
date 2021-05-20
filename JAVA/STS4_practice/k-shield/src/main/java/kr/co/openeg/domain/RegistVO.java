package kr.co.openeg.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class RegistVO {
	private Long rgno;
	private String userRole;
	private String name;
	private String email;
	private String password;
	private String salt;
	private String phone;
	private String content;
	private Date regDate;
	private Date updateDate;

	private List<RegistAttachVO> attachList = new ArrayList<RegistAttachVO>();
}
