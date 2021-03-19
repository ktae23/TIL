![image-20210319114756392](C:\Users\zz238\AppData\Roaming\Typora\typora-user-images\image-20210319114756392.png)

<br/>

- Servlet 
  - 맨땅

- WebContainer
  - Tomcat 등 web 설정 필요
  - 기반 시설

- Spring Framework
  - xml 등 프로젝트 설정 필요
  - 목조 구조

- Spring Boot
  - Tomcat 등의 설정마저 완료 됨
  - 반 제품

<br/>

## 26.3 스프링 애너테이션 이용해 로그인 기능 구현하기

> 중복 내용 제외하고 29장까지 진행 예정, 다음주 화요일(03.23)에 32장 스프링 부트 사용하기 진행 예정

<br/>

#### [예제 소스 제공](https://drive.google.com/drive/folders/18MqmrZ6lJoGTDDkAMtrWrBZVXUg3woFF)

- Spring project가 아닌 경우 예제 소스(SemiProject.zip) 다운로드 후 진행

- pom.xml을 확인하면 자바 1.6으로 되어 있음

  - 하위 호환이 제공 되기 때문
  - 1.6 이상 프로젝트에 지원 가능하다는 의미

- 프로젝트 우클릭 - maven - update project 이후 이클립스 껐다 키기

  - 이후에도 simple json 관련 에러 발생 시 아래 순서 대로 진행

- ```
  # properties -> library -> simple json 부분 제거 후 external jars에서 빌드패스 직접 설정
  
  # C -> 사용자 -> .mw -> .repository -> .com -> googlecode -> json-simple -> json - simple -> 1.1.1 -> json-simple-1.1.1.jar 
  
  # 프로젝트 우클릭 - maven - update project
  ```

<br/>

##### 기존 스프링 코드

``` java
public ModelAndView login2(@ReauestParam('userID') String userID, 
                           @ReqestParam("userName") String userName, 
                           HttpServletRequest request, HttpServletRespons response) throws Execption {
    
    String id = request.getParameter("userID");
    String name = request.getParameter("userName");

}
```

<br/>

​    위의 Request Param 애너테이션이 아래의 코드와 같은 기능을 수행한다.

<br/>

```java
public ModelAndView login2(@ReauestParam('userID') String userID, 
                           @ReqestParam("userName") String userName, 
                           HttpServletRequest request, HttpServletRespons response) throws Execption {
   
    System.out.print(userID);
    System.out.print(userName);
    
    
}
    ==============================
    /*
    String id = request.getParameter("userID");
    String name = request.getParameter("userName");
    
    */
```

<br/>

#### 26.3.2 @RequestParam의 required 속성 사용하기

- require 속성을 생략하면 기본값은 true로 필수 입력을 받아야 함.

  - null 체크가 자동으로 진행 됨

  - 필수 입력이 아닐 경우 문제 생길 수 있음

  - false 넣으면 필수 체크 해제

  - ```java
    @ReqestParam(value = "userName", required =true) 
    ```

<br/>

```java
@RequestMapping(value = "경로", method = "POST")
public ModelAndView login2(@ReauestParam('userID') String userID, 
                           @ReqestParam(value = "userName", required =true) String userName, 
                           HttpServletRequest request, HttpServletRespons response) throws Execption {
    
    System.out.print(userID);
    System.out.print(userName);
   
}
```

<br/>

#### 26.3.3 @RequestParam 이용해 Map에 매개변수 값 입력하기

```java
@RequestMapping(value = "경로", method = "POST")
public ModelAndView login2(@ReauestParam Map(String, String> info,
                           HttpServletRequest request, HttpServletRespons response) throws Execption {

    request.setCharaterEncoding("utf-8");
    ModelAndView mav = new ModelAndView();
    String userID = info.get("userID");
    String userName = info.get("UserName");
                             
	System.out.print(userID);
    System.out.print(userName);
                             
    mav.addObject("info",info);
    mav.setViewName("result");
    
    return mav;
}
```

<br/>

- 성능면에서는 기존 Request - Response 방식이 좋음
- 애너테이션 사용이 편하면 사용하겠지만 해석 및 사용하는데 시간이 걸린다면 추천하지 않음

<br/>

#### 26.3.4 @RequestParam 이용해 VO에 매개변수 값 입력하기

```java
@RequestMapping(value = "경로", method = "POST")
public ModelAndView login2(@ModelAttribute("info") LoginVO loginVO,
                           // 매개변수 값을 LoginVO클래스와 이름이 같은 속성에 자동으로 할당 
                           HttpServletRequest request, HttpServletRespons response) throws Execption {

    request.setCharaterEncoding("utf-8");
    ModelAndView mav = new ModelAndView();
    
    String userID = LoginVO.getUesrID();
    String userName = LoginVO.getUesrName();
                             
	String Example = memberService.login(loginVO);
    // @ModelAttribute로 들어온 설정값을 바로 loginVO 객체에 할당 되기 때문에 바로 사용 가능
                             
    //mav.addObject("info",info);
    mav.setViewName("result");
    
    return mav;
}
```

<br/>

#### 26.3.5 Model 클래스 이용해 값 전달하기

```java
@RequestMapping(value = "경로", method = "POST")
public String login2(Model model,
                           // 메서드 호출 시 Model 객체 자동 생성 
                           HttpServletRequest request, HttpServletRespons response) throws Execption {
	
    request.setCharaterEncoding("utf-8");
    //ModelAndView mav = new ModelAndView();
    model.addAttribute("userID", "hong");
    model.addAttribute("userName", "홍길동");
              
    
    return "result";
}
```

<br/>

넓은 의미에서 MVC의  Model은 data(VO Class) + business + DAO

엄밀한 의미에서 Model은 VO Class를 의미

<br/>

## 26.4 @Autowied 이용해 빈 주입하기

- XML에서 빈을 설정한 후 애플리케이션이 실행 될 때 빈을 주입해서 사용하면 복잡해지면서 사용 및 관리하기 어려워짐
- @Autowired의 특징
  - 기존 XML파일에서 각각의 빈을 DI로 주입했던 기능을 코드에서 애너테이션으로 자동 수행
  - 별도의 seetter나 생성자 없이 속성에 빈을 주입 할 수 있음
- 교재(자바 웹을 다루는 기술) 936p ~ XML 설정 부분 참고

<br/>

```java
@Controller("memberController")
public class MemberControllerImpl implemnets MemberController {
    @Autowired
    private MemberService memberService;
    
    메서드 () {
      ...   
    }
}
```

<br/>

- 단방향 통신으로 관계성 설계 필요
  - UI -> Controller -> Service -> DAO -> DB 
  - DB -> DAO -> Service -> Controller -> UI 
- VO 클래스는 데이터 관리를 위해 필요할 때마다 만들어서 사용하는 것 권장
  - 컨트롤러 클래스에 VO가 연결되어 있으면 안됨

<br/>

```java
@Repository("클래스 명 - 첫글자 소문자")
@Repository
public class Member{
    
}
================================
    위와 아래는 같음
================================
@Reposirory("member")
public class Member{
    
}
```

