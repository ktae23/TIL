## 기존 ibatis 프로젝트

- java 프로젝트를 maven으로 변경하면서 pom에는 ibatis 대신 mybatis dependency를 추가한 상황

---

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
    <util:properties id="config" location="classpath:config/dbconn.properties"/>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
    	<property name="driverClassName" value="#{config['jdbc.driver']}"/>
    	<property name="url" value="#{config['jdbc.url']}" />
    	<property name="username" value="#{config['jdbc.username']}" />
    	<property name="password" value="#{config['jdbc.password']}" />
    </bean>
    
    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer"> -->
    	<property name="locations" value="classpath:config/conn.properties;classth:config/database.properties" /> -->
    </bean> 
    
   	<bean id="sqlMapClient"
   		class="org.springframework.orm.ibatis.SqlMapClientFactoryBean">
   		<property name="dataSource" ref="dataSource" />
   		<property name="configLocation" value="classpath:/config/sqlMapConfig.xml" />	
   	</bean>
   	  	
   	<!-- Dao classes -->   
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

<br/>

### board.xml 

```xml-dtd
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE sqlMap      
    PUBLIC "-//ibatis.apache.org//DTD SQL Map 2.0//EN"      
    "http://ibatis.apache.org/dtd/sql-map-2.dtd">
    
<mapper namespace="board">
		<typeAlias alias="BoardModel" type="[패키지명].board.model.BoardModel"/>
		<typeAlias alias="BoardCommentModel" type="[패키지명].board.model.BoardCommentModel"/>	
			...
<select id="loginCheck" parameterClass="String" resultClass="LoginModel">
			...
	</select>	


</mapper>
```

<br/>

## Mybatis 프로젝트

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
       
    
    
    <mvc:annotation-driven/>
    <context:component-scan base-package="[패키지명]"/>

    
    <!-- viewResolver -->
    <bean id="viewResolver"
    	class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    	<property name="viewClass" value="org.springframework.web.servlet.view.JstlView" /> 	
   		<property name="prefix" value="/WEB-INF/" />
   		<property name="suffix" value=".jsp" />
   	</bean>
   	   	
 	

    <util:properties id="config" location="classpath:config/dbconn.properties"/>
    <bean id="dataSource" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
    	<property name="driverClassName" value="#{config['jdbc.driver']}"/>
    	<property name="url" value="#{config['jdbc.url']}" />
    	<property name="username" value="#{config['jdbc.username']}" />
    	<property name="password" value="#{config['jdbc.password']}" />
    </bean>
    
	<bean id="sqlSessionFactoryBean" class="org.mybatis.spring.SqlSessionFactoryBean">
		   <property name="dataSource" ref="dataSource" />
		   <property name="configLocation" value="classpath:/config/sqlConfig.xml" />
		   <property name="mapperLocations" value ="classpath:/mappers/*.xml"/>
	  </bean>	
 
	 <bean id="SqlSession" class="org.mybatis.spring.SqlSessionTemplate">
		<constructor-arg index="0" ref="sqlSessionFactoryBean" />
	 </bean>
	 

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

<br/>

### sqpConfig.xml

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

<br/>

