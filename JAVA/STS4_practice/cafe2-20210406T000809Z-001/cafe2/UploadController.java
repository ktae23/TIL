package com.mulcam.ai.web.controller;

import java.io.File;
import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {
	
	@PostMapping("/upload")
	public void upload(@RequestParam("file") MultipartFile file) {
		//이미지를 다시 후처리 해줘야 하는데 그것을 해주는 것이 MultipartFile 객체
		System.out.println(file);
		
		try {
			file.transferTo(new File("d:\\tool\\temp\\snapshot.jpg"));
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
