# K-Digital Training X 멀티 캠퍼스

#### AI 기반 지능형 서비스 개발 A반 세미 프로젝트 (개인) - 기획서(초안)

___

<br/>

#### 📌프로젝트 기간

---

#### 2021 - 03 - 15 - MON ~ 2021 - 03 - 18 - THU

- **03 - 15 - mon** : 오전 필기 시험 + 세미 프로젝트 설명, 기획서 제출 예정

- **03 - 17 - wed** : 포트폴리오 특강 예정
- **03 - 18 - thu** : 18:00 프로젝트 제출 및 일부 인원 발표

<br/>

- **프로젝트 실 진행 기간 약 3일 + a**

<br/>

#### 📌기술 스택

<details>
    <summary><strong>📁 개발 도구</strong></summary>
    <pre>
    📂 JDK 1.8
	📂 Eclipse 2020-12<br/>
	📂 STS 4<br/>
	📂 Springboot 2.4.3<br/>
	📂 mybatis <br/>
	📂 Oracle <br/>
	📂 Gradle 3.0<br/>
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

#### 🔐 계정

- 사용자 회원가입 / 로그인
   - 아이디, 패스워드 글자 제한
   - 중복 아이디 생성 불가
 - 한 계정에 게시물 10개만 작성 가능

<br/>

#### 💾 DB

- Oracle 사용
- 테이블
  - **User table**
    - mem_id
      - primary key
    - mem_pw
  - **User board table** (회원 가입 시 사용자 id와 같은 이름으로 테이블 생성)
    - mem_id
      - foreign key
    - button
    - url
    - create_date

<br/>

#### 📝 게시물

- 하이퍼링크가 걸린 버튼과 링크 설명이 하나의 게시물
- 게시물은 계정당 최대 10개 생성 가능
- 로그인 시 모든 게시물을 한 화면에 보여 줌

<br/>

<br/>

#### 선택 구현 기능

____

- 시간이 허락한다면 구현하고 싶은 기능들

> - REST API
>   - GET/POST/PUT/PATCH/DELETE
>   - 사실상 이해도 못했고 미천한 실력과 3일의 짧은 기간으로 구현 불가
> - 작성 날짜로 조회
> - 계정 당 게시물 생성 하루 10개 제한
> - 미리보기
>   - 작은 패널을 열어 해당 url의 index.html를 받아 와 보여주기

<br/>





