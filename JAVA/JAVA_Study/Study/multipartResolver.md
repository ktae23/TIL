1024 -> 1kb
1024k -> 1m

### 스프링 파일업로드

 ##### 1) commons-fileupload 라이브러리 pom.xml에 추가 해야함 (아래 참고)

```xml
 <!-- 파일 업로드 라이브러리 -->
	<!-- https://mvnrepository.com/artifact/commons-fileupload/commons-fileupload -->
	<dependency>
		<groupId>commons-fileupload</groupId>
		 <artifactId>commons-fileupload</artifactId>
		 <version>1.4</version>
	</dependency>
```



 ##### 2) servlet-context.xml파일에 아래 내용 추가 (10메가바이트로 고정함)

```xml
<beans:bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
	<beans:property name="maxUploadSize" value="10485760"/>
</beans:bean>
```



 ##### 3)servlet-context.xml파일에 파일 업로드 경로 리소스 설정

```xml
<resources mapping="/uploadimg/**" location="file:///D:/upload/" />
```



 ##### 4) 뷰jsp의 form의 설정 변경
파일 전송하기위한 설정 : method="post" enctype="multipart/form-data"



 ##### 5) 컨트롤러에서 파일 매개변수를 MultipartFile 형식으로 받음
ex)MultipartFile file1



 ##### 6) 파일업로드 메소드를 공용화 하기 위해 서비스 파일 생성
//파일을 폴더에 저장 하고 파일명을 리턴하는 메소드

```java 
	@Override
	public String fileupload(MultipartFile file) {
		//실제 파일이름
		String originfilename = file.getOriginalFilename();
		logger.info(originfilename);
        //파일네임 공백처리
        if(originfilename == "") return ""; //파일이 없다면 리턴

        //파일 이름이 겹치지 않도록 파일 이름 생성
        //천분의 1초의 시간을 파일 이름에 합쳐 중복이 안생기게 만듬
        String filename= System.currentTimeMillis() + originfilename;

        //파일 저장위치 설정
        File f= new File(uploadDir, filename);

        try {
            file.transferTo(f);  //파일 폴더에 저장

        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	
	return filename;
}
```

​			

 ##### 7) 받은 파일명만 따로 db저장


********************************************************

파일 화면에 보이기?



 ##### 1)위 3번의 경로 경로 설정 먼저 필수 



 ##### 2) 뷰 img (컨텍스트 패스 필수임)

<img alt="사진" src="${path}/uploadimg/${컨트롤러에서 받은 파일명}" width="100">