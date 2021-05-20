package kr.co.openeg.domain;

import lombok.Data;

@Data
public class RegistAttachVO {
	private String uuid;
	private String uploadPath;
	private String fileName;
	private boolean fileType;
	private Long rgno;
}
