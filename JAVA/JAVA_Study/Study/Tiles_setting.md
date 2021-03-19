## 27.9 타일즈란?

- JSP 페이지 레이아웃을 위한 프레임 워크
- pom.xml 의존성 주입으로 라이브러라 설치 및 사용 가능

```xml
<!-- 타일즈 관련 라이브러리 -->
		<dependency>
			<groupId>org.apache.tiles</groupId>
			<artifactId>tiles-core</artifactId>
			<version>2.2.2</version>
		</dependency>
		<dependency>
			<groupId>org.apache.tiles</groupId>
			<artifactId>tiles-jsp</artifactId>
			<version>2.2.2</version>
		</dependency>
		<dependency>
			<groupId>org.apache.tiles</groupId>
			<artifactId>tiles-servlet</artifactId>
			<version>2.2.2</version>
        </dependency>
```

<br/>

- 타일즈 사용시 기존에 사용한 InternalResourceViewResolver를 사용하지 않으므로 주석처리 함.
- src -> main -> webapp -> WEB-INF -> spring -> appServlet -> servlet-context.xml에서 타일즈 기능에 관련된 빈 설정

```xml
<!-- 뷰 리졸버 주석처리 -->
<!-- 
	<beans:bean class="org.springframework.web.servlet.view.InternalResourceViewResolver"> 
		<beans:property name="prefix" value="/WEB-INF/views/" /> 
		<beans:property name="suffix" value=".jsp" /> 
	</beans:bean>
  <context:component-scan	base-package="com.myspring.pro27" />
-->


<!-- 타일즈 실습 설정 -->
    <beans:bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles2.TilesConfigurer">
		<beans:property name="definitions">
			<beans:list>
				<beans:value>classpath:tiles/*.xml</beans:value>
			</beans:list>
		</beans:property>
		<beans:property name="preparerFactoryClass"
			          value="org.springframework.web.servlet.view.tiles2.SpringBeanPreparerFactory" />
	</beans:bean>
	<beans:bean id="viewResolver"
		class="org.springframework.web.servlet.view.UrlBasedViewResolver">
		<beans:property name="viewClass" value="org.springframework.web.servlet.view.tiles2.TilesView" />
	</beans:bean>

```

- TilesConfigurer 클래스 빈을 생성하면서 URL 요청에 대해 브라우저에 나타낼 정보가 저장된 타일즈 설정 파일을 패키지 tiles에서 읽어 들임

<br/>

##### 교재 tiles.xml 예제 소스

- 교재(자바 웹을 다루는 기술) 990p ~ 참조

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE tiles-definitions PUBLIC
"-//Apache Software Foundation//DTD Tiles Configuration 2.0//EN"
"http://tiles.apache.org/dtds/tiles-config_2_0.dtd">
<tiles-definitions>
   <definition name="baseLayout"  template="/WEB-INF/views/common/layout.jsp">
      <put-attribute name="title" value="" />
      <put-attribute name="header" value="/WEB-INF/views/common/header.jsp" />
      <put-attribute name="side" value="/WEB-INF/views/common/side.jsp" />
      <put-attribute name="body" value="" />
      <put-attribute name="footer" value="/WEB-INF/views/common/footer.jsp" />
   </definition>

   <definition name="main" extends="baseLayout">
      <put-attribute name="title" value="메인페이지" />
      <put-attribute name="body" value="/WEB-INF/views/main.jsp" />
   </definition>


   <definition name="/member/listMembers" extends="baseLayout">
      <put-attribute name="title" value="회원목록창" />
      <put-attribute name="body" value="/WEB-INF/views/member/listMembers.jsp" />
   </definition>
   

    
   <definition name="/member/loginForm" extends="baseLayout">
      <put-attribute name="title" value="로그인창" />
      <put-attribute name="body" value="/WEB-INF/views/member/loginForm.jsp" />
   </definition>
      
<!-- 
   <definition name="/member/memberForm" extends="baseLayout">
      <put-attribute name="title" value="회원등록창" />
      <put-attribute name="body" value="/WEB-INF/views/member/memberForm.jsp" />
   </definition>
   <definition name="/member/viewDetail" extends="baseLayout">
      <put-attribute name="title" value="회원상세창" />
      <put-attribute name="body" value="/WEB-INF/views/member/viewDetail.jsp" />
   </definition> -->
</tiles-definitions>
```

