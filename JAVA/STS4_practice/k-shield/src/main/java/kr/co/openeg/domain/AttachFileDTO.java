package kr.co.openeg.domain;

import lombok.Data;

@Data
public class AttachFileDTO {
	private String uuid;
	private String uploadPath;
	private String fileName;
	private boolean image;
	
	public String getUuid() {
		if (uuid == null) 
			return "";
		else 
			return this.uuid;
	}
}
