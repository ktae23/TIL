# K-Digital Training X 멀티 캠퍼스

#### AI 기반 지능형 서비스 개발 A반 세미 프로젝트 (개인) - 결과

___

<br/>

#### 📌프로젝트 기간

---

#### 2021 - 03 - 15 - MON ~ 2021 - 03 - 18 - THU

- **03 - 15 - mon** : 오전 필기 시험 + 세미 프로젝트 설명
- **03 - 17 - wed** : 포트폴리오 특강
- **03 - 18 - thu** : 일부 인원 발표
- **03 - 19 - thu** ~ **03 - 24 - mon** : 기획안(`*.word`), 결과보고서(`*.ppt`), 시연 영상(`youtube`) 제출

<br/>

- **프로젝트 실 진행 기간 약 3일 + a**

<br/>

#### 📌기술 스택

<details>
    <summary><strong>📁 개발 도구</strong></summary>
    <pre>
    📂 JDK 1.8<br/>
	📂 Eclipse 2020-12<br/>
	📂 SpringMVC project<br/>
	📂 Spring 5.0.7.RELEASE<br/>
	📂 mybatis 3.1.0<br/>
	📂 OracleXE <br/>
	📂 maven 2.5.1<br/>
	📂 BootStrap 4
	</pre>
</details>
<details>
    <summary><strong>📁 Git</strong></summary>
    <pre>
   📂 <a href="https://github.com/ktae23/KDT_SemiProject.git">Git-Hub</a>
    </pre>
</details>

<br/>

#### 📌구현 기능

___

#### 💻 인프라

- 톰캣을 이용한 localhost 서버 이용

<br/>

####  📋 도메인

- 스크랩 게시판
  - 원하는 URL과 해당 URL에 이름을 정해 함께 입력하면 하나의 게시물로 저장 됨
    - URL에 대한 짧은 설명을 함께  작성하여 저장
  - 게시물 작성, 조회, 삭제, 수정의 CRUD 기능 구현
   - 북마크 기능을 게시판으로 구현

<br/>

🟢 인덱스 페이지

<br/>

![index](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/mainpage1.png)

![로그인 후](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/mainpage2.png)



<br/>

#### 🔐 계정

- 사용자 회원가입 , 로그인, 회원 조회, 삭제의 CRUD 기능 구현
  - 아이디, 패스워드 글자 제한
  - 중복 아이디 생성 불가
 - 계정 삭제 시 작성한 게시물 전체 삭제
 - 관리자 계정과 별도 회원관리 페이지 제작
    - 회원 정보 전체 열람 및 CRUD 권한

<br/>

🟢 로그인, 로그아웃

<br/>

![로그인](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberIdCheck.png)

![로그아웃](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/logout.png)

<br/>

🟢 회원 CRUD

<br/>

![회원 가입](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberInsert.png)

![회원 리스트](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberList.png)

![회원 수정](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberupdate.png)

![회원 삭제1](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberDelete1.png)

![회원 삭제2](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberDelete2.png)

![회원 삭제3](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/memberDelete3.png)

<br/>

🟢 관리자 페이지

<br/>

![관리자](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/admin_mainpage.png)

![관리자 회원 관리](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/admin_memberList.png)



<br/>

#### 📝 게시물

- 하이퍼링크가 걸린 타이틀과 짧은 코멘트가 하나의 게시물
  - 글 번호, 작성자 id와 게시일이 함께 작성 됨
- 게시판 조회 시 모든 게시물을 한 화면에 보여 줌
- 자신이 생성한 게시물에 대해 pw 입력 후 수정 및 삭제 가능
- 미 로그인시 게시물 조회 불가

<br/>

🟢 북마크 CRUD

<br/>

![북마크 생성](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/bookmarkInsert.png)

![북마크 리스트](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/bookmarkList.png)

![북마크 수정](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/bookmarkUpdate.png)

![북마크 삭제](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/bookmarkDelete.png)

<br/>



#### 💾 DB

- Oracle SQL Developer 사용
- 테이블 및 컬럼
  - CREATE TABLE `member` (
    - `id` NVARCHAR2(20) NOT NULL PRIMARY KEY
    - `pw` NVARCHAR2(20) NOT NULL
    - `memname` NVARCHAR2(20)
    - `memdate` DATE DEFAULT SYSDATE
    - );
  - CREATE TABLE `bookmarks` (회원 가입 시 사용자 id와 같은 이름으로 테이블 생성)
    - `title` NVARCHAR2(20)
    - `coment` NVARCHAR2(500) NOT NULL
    - `url` NVARCHAR2(200) NOT NULL
    - `create_date` DATE DEFAULT SYSDATE
    - `mem_id` NVARCHAR2(20) NOT NULL FOREIGN KEY
    - `bookmark_no` NUMBER NOT NULL
    - );
    - 북마크 글 번호를 위한 시퀀스 생성
      - CREATE SEQUENCE `bookmark_no_seq` START WITH 1 INCREMENT BY 1;

<br/>

![erd](https://github.com/ktae23/KDT_SemiProject/blob/master/semi_project_result_imgs/erd.png)



<br/>

#### 느낀 점

____

- 난 정말 ㅈ밥이다.

  - > 수많은 에러의 향연 속에서 난 길을 잃기도 했다.
    >
    > "내가 개발자가 될 수 있을까..? "
    >
    > "나 공부 허투루 했네? 이런 애를 돈 주고 일시키는 데가 있을까?"
    >
    >
    > 정말 수 차례 그만두려다 이 악물고 구글링해서 에러 해결하고.. 해결하고...
    >
    > 
    >
    > 
    >
    > 1차 고비는 스프링부트에서 아무리 발악해도 DB 연결이 안되는거다.
    >
    > 설정에서 Datasource를 url이 먼저오게 순서만 바꿔주니까 되더라.. 1차 멘붕
    > 
    >
    > 2차 고비는 연결은 됐는데 SQL 실행이 안되는거다.
    >
    > DBCP 설정 문제일 것 같다는 정도까진 알아낸것 같은데 스프링 부트를 써본 적이 없어서 도무지 어떻게 설정해야 할지 모르겠었다.
    >
    > 처음에 프로젝트 생성 시부터 했어야 했나?
    > 
    >
    > 그렇게 다음날 오후 1시에 프로젝트를 갈아 엎고 legacy SpringMVC 프로젝트로 다시 시작했다.
    >
    > 이후 2시간은 xml에 ojdbc 연결 문제로 씨름하고..
    >
    > 저녁엔 Sql mapper.xml 문제로 3시간 삽질하고..ㅎㅎ
    > 
    >
    >
    > 그래도 에러 잔뜩 보다가 한번 쿼리 날려주면 그렇게 기뻤다. 
    >
    > 정말 감정 기복이 심한 이틀이었다.
    >
    > ~~이래서 나쁜 남자, 나쁜 여자한테 끌리는건가..?~~
    >
    > 그래도 남은 이틀 동안 4시간 정도만 자면서 몰두해서 정말 염치 불구할 정도의 CRUD는 구현했다.
    > 확실히 이번 프로젝트를 통해서 에러 잡기, 구글링, 설정 읽기, 어떻게 요청을 주고 받는지 그 구조 등이 보였다.
    >
    > 정말 난 작은 점 같은 실력을 가졌고,  **자료구조, 알고리즘, HTTP 통신, Web 구조, 코딩 테스트, TDD, Git flow... 등등** 
    > 앞으로 공부 해야 할 공부가 너무 너무 많고 4일간 의자 -> 침대 -> 의자 -> 침대 뿐이었지만 정말 재밌었다.
    >
    >
    > 내가 무언갈 만들었고, 그게 동작한다는 것도 재밌지만
    > 풀리지 않는 문제에 맞닥드렸을 때 이를 찾아서 해결하고 새로운 지식이 생기는 과정이 너무 재밌다.
    >
    >
    > 방에 처박혀서 4일 간 집 밖에 한 발짜국도 안나갔는데도 답답하지도 않고 친구들이 보자고 부르는 시간도 아까울 정도였다.
    > 내 지식과 실력이 일천하지만 개발이란 것을 계속 하고 싶은 욕심이 난다.

<br/>

#### 아쉬운 점

____

- 시간이 허락한다면 구현하고 싶었던 기능들

> - REST API
>
>   - GET/POST/PUT/PATCH/DELETE
>
>     - > 제대로 배우지도 않은 스프링부트로 공부해가며 하겠다고 초반에 이틀 날려먹지만 않았어도 애너테이션 정도는 달 수 있었다.
>
> - 작성 날짜로 조회
>
>   - 데이터베이스에서 받아오는 SYSDATE 포맷팅만 잘 하면 가능하다
>
>     - > 캘린더 클래스를 이용해서 날짜 선택 시 해당 날짜 게시물만 조회하는 건 시간만 더 있었으면 가능하다.
>
> - 계정 당 게시물 생성 하루 10개 제한
>
>   - 게시물 생성때마다 카운트해서 00시 기점으로 리셋을 해주면 되는데...
>
>     - > 지금 생각해보면 굳이 이런 기능이 있어야하나 싶은데 프리미엄 모델로의 비즈니스를 생각한다면 필요하겠지만...
>       >
>       > 북마크를 게시물 형태로 하겠다고 돈을 낸다고?
>
> - 미리보기
>
>   - 작은 패널을 열어 해당 url의 index.html를 받아 와 보여주기
>
>     - > OG태그를 긁어와 파싱하면 되지만... 어떻게 보면 북마크 게시판에서 가장 매력적인 기능이 될 순 있겠지만,
>       >
>       >  서버 부담이나 크롤러 구현이 본체 기능보다 구현이 복잡 할지도;;

<br/>





