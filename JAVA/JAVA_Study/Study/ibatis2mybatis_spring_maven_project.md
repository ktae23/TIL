## 기존 ibatis 프로젝트

- java 프로젝트를 maven으로 변경하면서 pom에는 ibatis 대신 mybatis dependency를 추가한 상황

---

### pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>[프로젝트 이름]</groupId>
  <artifactId>[프로젝트 이름]</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>war</packaging>

  <dependencies>
  	<!-- https://mvnrepository.com/artifact/org.aspectj/aspectjweaver -->
	<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.0</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-context -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.2.9.RELEASE</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api -->
	<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api -->
	<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-web -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-webmvc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-jdbc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-jdbc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.2.2</version>
	</dependency>

	<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>1.2.0</version>
	</dependency>

  </dependencies>
  
  <build>
            ...
  </build>
</project>
```

<br/>

- 프로젝트가 구동하면 web.xml을 먼저 읽은 후 servlet을 읽어 온다.

### web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
xmlns="http://java.sun.com/xml/ns/javaee" 
xmlns:web="http://java.sun.com/xml/ns/javaee" 
xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" 
id="WebApp_ID" 
version="2.5">

  <display-name>[프로젝트 이름]</display-name>

  	<context-param>
		<param-name>contextConfigLocation</param-name>
		<param-value>
			/WEB-INF/spring-security.xml
		</param-value>
	</context-param>
	
  <servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
  </servlet>
  
  <servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>*.do</url-pattern>
  </servlet-mapping>
  

</web-app>
```

<br/>

- web.xml에서 읽어들인 servlet인 dispatcher-sevlet에서는 bean을 정의한다

### dispatcher-servlet

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:context="http://www.springframework.org/schema/context" 
	xmlns:mvc="http://www.springframework.org/schema/mvc" 
	xmlns:p="http://www.springframework.org/schema/p"
	xmlns:aop="http://www.springframework.org/schema/aop"
	xmlns:util="http://www.springframework.org/schema/util"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
       http://www.springframework.org/schema/util
       http://www.springframework.org/schema/util/spring-util-3.0.xsd
       http://www.springframework.org/schema/mvc
       http://www.springframework.org/schema/mvc/spring-mvc-3.0.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop-3.1.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-3.0.xsd">   
    
    <context:component-scan base-package="[패키지명].board.controller"/>		
    <context:component-scan base-package="[패키지명].board.service"/>	
    <context:component-scan base-package="[패키지명].init"/>	
    <context:property-placeholder location="/WEB-INF/*.properties" /> 

    
    <!-- viewResolver -->
    <bean id="vewResolver"
    	class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    	<property name="viewClass" value="org.springframework.web.servlet.view.JstlView" /> 	
   		<property name="prefix" value="/WEB-INF/" />
   		<property name="suffix" value=".jsp" />
   	</bean>
	
   	 <!-- iBatis -->
     <!-- JDBC 커넥트 관리 부분으로 config 패키지 하위 properties로 정보가 관리 되고 있었따.-->
    <util:properties id="config" location="classpath:config/dbconn.properties"/>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
    	<property name="driverClassName" value="#{config['jdbc.driver']}"/>
    	<property name="url" value="#{config['jdbc.url']}" />
    	<property name="username" value="#{config['jdbc.username']}" />
    	<property name="password" value="#{config['jdbc.password']}" />
    </bean>

    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer"> 
    	<property name="locations" value="classpath:config/conn.properties;classth:config/database.properties" /> 
    </bean> 
    
    <!-- ibatis 사용을 위한 SqlMapClient 팩토리 빈으로 바로 위 datasouorce 빈과 config 패키지 하위 sqlMapConfig 설정 xml을 참조 한다-->
   	<bean id="sqlMapClient"
   		class="org.springframework.orm.ibatis.SqlMapClientFactoryBean">
   		<property name="dataSource" ref="dataSource" />
   		<property name="configLocation" value="classpath:/config/sqlMapConfig.xml" />	
   	</bean>
   	  	
   	<!-- Dao classes -->   
    <!-- Dao 클래스 사용을 위한 빈 생성-->
	<bean id="loginDao"
    	class="[패키지명].login.dao.LoginDaoImpl" 
    	p:sqlMapClient-ref="sqlMapClient" />
 	
 	<bean id="memberDao"
 		class="[패키지명].member.dao.MemberDaoImpl" 
    	p:sqlMapClient-ref="sqlMapClient" />
 	
 	<bean id="boardDao"
 		class="[패키지명].board.dao.BoardDaoImpl" 
    	p:sqlMapClient-ref="sqlMapClient" />


</beans>
```

<br/>

### BoardDaoImpl

```java
package kr.co.openeg.lab.board.dao;

import java.util.HashMap;
import java.util.List;

import [패키지명].board.model.BoardCommentModel;
import [패키지명].board.model.BoardModel;

import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;


public class BoardDaoImpl extends SqlMapClientDaoSupport  implements BoardDao {

	private HashMap<String, Object> valueMap = new HashMap<String, Object>();
		

	@Override
	public List<BoardCommentModel> selectCommentList(int idx) {
		return getSqlMapClientTemplate().queryForList("board.getCommentList", idx);
	}

	@Override
	public void insertArticle(BoardModel board) {
		    getSqlMapClientTemplate().insert("board.writeArticle", board);	
	}

	@Override
	public void deleteArticle(int idx) {
		getSqlMapClientTemplate().delete("board.deleteArticle", idx);	
	}

	@Override
	public BoardCommentModel selectOneComment(int idx) {
		return (BoardCommentModel) getSqlMapClientTemplate().queryForObject("board.getOneComment", idx);		
	}

	@Override
	public void updateArticle(BoardModel board) {
		   getSqlMapClientTemplate().update("board.modifyArticle", board);
	}	

}

```

- ibatis 메서드
  - **queryForObject** : SELECT로 값을 가져올 때 사용, 데이터가 여러 행일 경우 자동으로 List 형태로 변환
  - **queryForList** : SELECT로 값을 가져올 때 사용, 조건에 따라 데이터가 하나일 경우 quereyForObject를 사용
  - **insert**
  - **update**
  - **delete**

[참고 블로그](https://krespo.net/101)



<br/>

### sqlMapConfig.xml

```xml-dtd
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE sqlMapConfig      
    PUBLIC "-//ibatis.apache.org//DTD SQL Map Config 2.0//EN"      
    "http://ibatis.apache.org/dtd/sql-map-config-2.dtd">

<sqlMapConfig>
	<settings useStatementNamespaces="true"	/>	
	<sqlMap resource="[패키지명]/member/dao/member.xml" />
	<sqlMap resource="[패키지명]/board/dao/board.xml" />
	<sqlMap resource="[패키지명]/login/dao/login.xml" />
</sqlMapConfig>
```

- 매퍼 파일이 있는 위치를 지정해준다.
- DOCTYPE이 sqlMApConfig이다.

<br/>

### board.xml 

```xml-dtd
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE sqlMap      
    PUBLIC "-//ibatis.apache.org//DTD SQL Map 2.0//EN"      
    "http://ibatis.apache.org/dtd/sql-map-2.dtd">
    
<sqlMap namespace="board">
		<typeAlias alias="BoardModel" type="[패키지명].board.model.BoardModel"/>
		<typeAlias alias="BoardCommentModel" type="[패키지명].board.model.BoardCommentModel"/>	
			...
<select id="loginCheck" parameterClass="String" resultClass="LoginModel">
			...
	</select>	


</sqlMap>
```

- ibatis는 DOCTYPE과 최상위 태그가 sqlMap이다.
- sqlMap의 namespace는 줄임말로 사용 가능
- parameteClass와 resultClass를 사용한다
  - 여기서 사용되는 model(VO,DTO)의 별칭은 typeAlias 지정을 통해 지정한다.

<br/>

## Mybatis 프로젝트

### pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" 
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
https://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>
  <groupId>[프로젝트명]</groupId>
  <artifactId>[프로젝트명]</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>war</packaging>

  	
  <dependencies>
    <dependency>
         <groupId>javax.servlet</groupId>
         <artifactId>jstl</artifactId>
         <version>1.2</version>
    </dependency>
    <dependency>
      <groupId>javax.servlet.jsp</groupId>
      <artifactId>javax.servlet.jsp-api</artifactId>
      <version>2.3.0</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>javax.el</groupId>
      <artifactId>javax.el-api</artifactId>
      <version>2.2.2</version>
      <scope>provided</scope>
    </dependency>
  	<!-- https://mvnrepository.com/artifact/org.aspectj/aspectjweaver -->
	<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.0</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-context -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.2.9.RELEASE</version>
	</dependency>

	<!-- https://mvnrepository.com/artifact/javax.servlet/javax.servlet-api -->
	<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/javax.annotation/javax.annotation-api -->
	<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-web -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<!-- https://mvnrepository.com/artifact/org.springframework/spring-webmvc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>

	<!-- https://mvnrepository.com/artifact/org.springframework/spring-jdbc -->
	<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.0.7.RELEASE</version>
	</dependency>
	
	<dependency>
    	<groupId>commons-dbcp</groupId>
    	<artifactId>commons-dbcp</artifactId>
    	<version>1.4</version>
	</dependency>
	
	<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>1.3.2</version>
	</dependency>	
	
	<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.4.6</version>
	</dependency>

 <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-core</artifactId>
        <version>4.1.3.RELEASE</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-web</artifactId>
        <version>4.1.3.RELEASE</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-config</artifactId>
        <version>4.1.3.RELEASE</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-taglibs</artifactId>
        <version>4.1.3.RELEASE</version>
    </dependency>


	     <!-- https://mvnrepository.com/artifact/mysql/mysql-connector-java -->
    <dependency>
	     <groupId>mysql</groupId>
	     <artifactId>mysql-connector-java</artifactId>
	     <version>8.0.22</version>
     </dependency>


  </dependencies>
  
  <build>
               ...
  </build>
</project>
```

<br/>

### web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
xmlns="http://java.sun.com/xml/ns/javaee" 
xmlns:web="http://java.sun.com/xml/ns/javaee" 
xsi:schemaLocation="http://java.sun.com/xml/ns/javaee: http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" 
id="WebApp_ID" 
version="2.5">

  <display-name>[프로젝트명]</display-name>

  	<context-param>
		<param-name>contextConfigLocation</param-name>
		<param-value>
			/WEB-INF/spring-security.xml
		</param-value>
	</context-param>
  
  <servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
	  <init-param> 
	  	<param-name>contextConfigLocation</param-name> 
	  	<param-value>/WEB-INF/dispatcher-servlet.xml</param-value> 
	  </init-param> 
	  <load-on-startup>1</load-on-startup> 
  </servlet>
  
    <servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>*.do</url-pattern>
  </servlet-mapping>

</web-app>
```

<br/>

### dispatcher-servlet

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:context="http://www.springframework.org/schema/context" 
	xmlns:mvc="http://www.springframework.org/schema/mvc" 
	xmlns:p="http://www.springframework.org/schema/p"
	xmlns:aop="http://www.springframework.org/schema/aop"
	xmlns:util="http://www.springframework.org/schema/util"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
       http://www.springframework.org/schema/util
       http://www.springframework.org/schema/util/spring-util-3.0.xsd
       http://www.springframework.org/schema/mvc
       http://www.springframework.org/schema/mvc/spring-mvc-3.0.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop-3.1.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-3.0.xsd">   
       
    
    <!-- 어노테이션을 사용하기 위한 태그, 바로 윗 태그인 beans에 xmlns:mvc가 있어야 함-->
    <mvc:annotation-driven/>
    <!-- 스캔 할 컴포넌트(어노테이션)의 상위 패키지 명시 -->
    <context:component-scan base-package="[패키지명]"/>

    
    <!-- viewResolver -->
    <bean id="viewResolver"
    	class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    	<property name="viewClass" value="org.springframework.web.servlet.view.JstlView" /> 	
   		<property name="prefix" value="/WEB-INF/" />
   		<property name="suffix" value=".jsp" />
   	</bean>
   	   	
 	
	<!-- dataSource는 동일-->
    <util:properties id="config" location="classpath:config/dbconn.properties"/>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
    	<property name="driverClassName" value="#{config['jdbc.driver']}"/>
    	<property name="url" value="#{config['jdbc.url']}" />
    	<property name="username" value="#{config['jdbc.username']}" />
    	<property name="password" value="#{config['jdbc.password']}" />
    </bean>
    
    <!-- mybatis 사용을 위한 sqlSession 팩토리 빈으로 바로 위 datasouorce 빈과 config 패키지 하위 sqlConfig 설정 xml, 그리고 mapper를 모아 둔 mappers 패키지 하위 모든 xml 을 참조한다.-->
	<bean id="sqlSessionFactoryBean" class="org.mybatis.spring.SqlSessionFactoryBean">
		   <property name="dataSource" ref="dataSource" />
		   <property name="configLocation" value="classpath:/config/sqlConfig.xml" />
		   <property name="mapperLocations" value ="classpath:/mappers/*.xml"/>
	  </bean>	
 
	 <bean id="SqlSession" class="org.mybatis.spring.SqlSessionTemplate">
		<constructor-arg index="0" ref="sqlSessionFactoryBean" />
	 </bean>
	 <!-- <mvc:annotation-driven/>과 <context:component-scan base-package="[패키지명]"/>을 사용하기 때문에 따로 dao를 bean으로 잡지 않는다.-->

</beans>
```

<br/>

### BoardDao

```java
package kr.co.openeg.lab.board.dao;

import java.util.HashMap;
import java.util.List;

import [패키지명].board.model.BoardCommentModel;
import [패키지명].board.model.BoardModel;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BoardDao{

	private HashMap<String, Object> valueMap = new HashMap<String, Object>();
	
    //  <mvc:annotation-driven/>과 <context:component-scan base-package="[패키지명]"/>을 사용하기 때문에 @Autowired 어노테이션을 다는것 만으로 주입이 된다.
	@Autowired
	private SqlSession sqlSession;

	private static final String mapper = "[패키지명].board.dao.BoardDao";
	 
	 
	public List<BoardCommentModel> selectCommentList(int idx) {
		return sqlSession.selectList(mapper+".getCommentList", idx);
	}

	 
	public void insertArticle(BoardModel board) {
		    sqlSession.insert(mapper+".writeArticle", board);	
	}
	
	 
	public void deleteArticle(int idx) {
		sqlSession.delete(mapper+".deleteArticle", idx);	
	}

	 
	public BoardCommentModel selectOneComment(int idx) {
		return (BoardCommentModel) sqlSession.selectOne(mapper+".getOneComment", idx);		
	}

	 
	public void updateArticle(BoardModel board) {
		   sqlSession.update(mapper+".modifyArticle", board);
	}	

}

```

- mybaits 기본 메서드
  - selectOne
  - selectList
  - insert
  - update
  - delete

<br/>

### sqlConfig.xml

```xml-dtd
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
	<typeAliases>
		<typeAlias type="[패키지명].login.model.LoginHistory" alias="LoginHistory"/>
		<typeAlias type="[패키지명].login.model.LoginSessionModel" alias="LoginSessionModel"/>
		<typeAlias type="[패키지명].board.model.BoardCommentModel" alias="BoardCommentModel"/>
		<typeAlias type="[패키지명].board.model.BoardModel" alias="BoardModel"/>
		<typeAlias type="[패키지명].member.model.MemberModel" alias="MemberModel"/>
		<typeAlias type="[패키지명].member.model.MemberSecurity" alias="MemberSecurity"/>
	</typeAliases>
	
<!-- 	<mappers>
		<mapper resource="mappers/member.xml"/>
		<mapper resource="mappers/login.xml"/>
		<mapper resource="mappers/board.xml"/>
	</mappers>	 -->
</configuration>
```

- sqlSessionFactorybean에서 mapperLocation을 설정 했기 때문에 \<mapper> 태그를 작성하지 않음
- 대신 model(VO,DTO)에 대한 typeAlias를 지정하여 mapper(xml)에서 사용

<br/>

### board.xml 

```xml-dtd
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    
<mapper namespace="[패키지명].board.dao.BoardDao">
			...
	<select id="getCommentList" parameterType="int" resultType="BoardCommentModel">
			...
	</select>
	<update id="modifyArticle" parameterType="BoardModel">	
			...
	</update>
</mapper>
```

- DOCTYPE과 최상위 태그가 mapper이다.
- namespace 작성이 필수이며 Dao를 풀네임으로 적는다
- parameteType, resultType을 사용한다
  - 여기서 사용하는 이름은 sqlConfig.xml에서 설정한 typeAlias이다.

<br/>

