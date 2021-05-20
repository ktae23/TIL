### image download Controller

```java
package com.proto.mm.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


import com.proto.mm.model.Movie;

import com.proto.mm.model.Poster;
import com.proto.mm.service.PosterService;

@Controller
public class PosterController {
	
	@Autowired
	PosterService posterService;
	
	
	@RequestMapping(value="download", 
			method= {RequestMethod.GET})
    // movieTitle 값을 파라미터로 받아 오도록 @RequestParam 어노테이션 사용
    public void downLoad(@RequestParam String movieTitle, ModelMap model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
        	
		  posterService.posterDownload(model, request, response);
		  
          Movie movie = (Movie) model.getAttribute("movie");
   		  // 포스터 서비스에서 뽑아온 파일 이름을 꺼낸다
		  String dFile = (String) model.getAttribute("dFile");
		  
	      // File.separator를 이용하면 운영체제에 맞는 `/`로 사용 할 수 있다.
		  // 윈도우 상 경로라면 "/" 대신 드라이브 경로를 적어주면 된다. 
          // String upDir = "C:"+ File.separator +"Users" + File.separator + "user" + File.separator + MM"+File.separator+"movie_imgs";
        
	      String upDir = "/"+ File.separator + "MM"+File.separator+"movie_imgs";
		  String path = upDir+File.separator+dFile;
		  
		  File file = new File(path);
		  
          // 사용자 에이전트 값을 가져와서 아래 조건문을 통해 브라우저에 맞게 설정한다
		  String userAgent = request.getHeader("User-Agent");
		  boolean ie = userAgent.indexOf("MSIE") > -1 || userAgent.indexOf("rv:11") > -1;
		  String fileName = null;
		  
          // 영화 제목에 간단한 정규식을 사용한 뒤 파일 이름으로 다시 설정 해준다.
		  String tmp = movie.getMovieTitle().replace(" ", "").replace(":","_") + ".png";
		  
		  if (ie) {
		   fileName = URLEncoder.encode(tmp, "utf-8");
		  } else {
		   fileName = new String(tmp.getBytes("utf-8"),"iso-8859-1");
		  }
		  
          // 이 아래는 파일을 실어 보내는 작업
		  response.setContentType("application/octet-stream");
		  response.setHeader("Content-Disposition","attachment;filename=\"" +fileName+"\";");
		  
		  FileInputStream fis=new FileInputStream(file);
		  BufferedInputStream bis=new BufferedInputStream(fis);
		  ServletOutputStream so=response.getOutputStream();
		  BufferedOutputStream bos=new BufferedOutputStream(so);
		  
		  byte[] data=new byte[2048];
		  int input=0;
		  while((input=bis.read(data))!=-1){
		   bos.write(data,0,input);
		   bos.flush();
		  }
		  
		  if(bos!=null) bos.close();
		  if(bis!=null) bis.close();
		  if(so!=null) so.close();
		  if(fis!=null) fis.close();
		 }
}
```

<br/>

### image download Service

```java
public ModelMap posterDownload(ModelMap model, HttpServletRequest request, HttpServletResponse response) throws IOException {

	// 영화 제목으로 영화 코드를 뽑아오는 작업
    String movieTitle = request.getParameter("movieTitle");
    Movie movie = movieRepository.findByMovieTitle(movieTitle);
    BigDecimal movieCode = movie.getMovieCode();

    // 파일 이름에 확장자 붙이기(파일 이름이 영화 코드로 되어 있다)
    String dFile = movieCode + ".png";
    
    model.addAttribute("dFile", dFile);
    model.addAttribute("movie", movie);

    return model;
}
```

