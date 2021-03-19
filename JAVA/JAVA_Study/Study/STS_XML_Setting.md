## 27 메이븐과 스프링 STS 사용법

- 교재(자바 웹을 다루는 기술)  948p 메이븐 설치 및 STS 설치, 설정 방법 참조

<br/>

#### 27.6 STS 프로젝트 실행하기

- Oracle은 dependency 설정만으로 의존성 주입이 되지 않음
  - ojdbc를 WEB-INF -> lib에 직접 넣어줘야 함
- action-sevlet.xml
  - Spring과 mybatis 를 연결해 줌
  - config, mapper 설정
  - web.xml에 context - param 태그에 경로 설정 되어 있음

<br/>

#### Context의 Parameter 종류

- User parameter
  - request.getParameter를 이용
  - 사용자로부터 넘어오는 데이터를 매 요청마다 읽음
- sevlet init Parameter
  - web.xml -> Servlet태그 -> init-param태그
  - 해당 서블릿이 객체화(초기화) 될 때 한 번 읽음 
- Sevlet Context Parameter
  - Web Context와 같이 취급 됨
  - web.xml의 context - param태그로 다른 태그에 속하지 않는 초기 설정 파라미터, Context 실행 시 가장 먼저 읽음
  - -> Web Container(Tomcat)에 여러 Web Context가 접근하여 사용 가능
  - Run On Server 했을 때 여러 프로젝트(Web Context)가 올라와 있는 것을 확인 할 수 있음

<br/>

###  27.7.2 마이바티스 관련 XML 파일 추가하기

##### jdbc.properies

WEB-INF -> configure(파일 만들기) - jdbc.properies

```xml
jdbc.driverClassName=oracle.jdbc.driver.OracleDriver
jdbc.url=jdbc:oracle:thin:@localhost:1521:XE
jdbc.username=[DB 이름]
jdbc.password=[비밀번호]

```

<br/>

##### jdbc 설정

```xml
	<bean id="dataSource" class="org.apache.ibatis.datasource.pooled.PooledDataSource">
		<property name="driver" value="${jdbc.driverClassName}"></property>
		<property name="url" value="${jdbc.url}"></property>
		<property name="username" value="${jdbc.username}"></property>
		<property name="password" value="${jdbc.password}"></property>
	</bean>
```

<br/>

##### mapper 파일 설정

```xml
<bean id="sqlSessionFactoryBean" class="org.mybatis.spring.SqlSessionFactoryBean">
		<property name="dataSource" ref="dataSource"></property>
		<property name="configLocation" value="classpath:mybatis/model/modelConfig.xml"></property>
		<property name="mapperLocations" value="classpath:mybatis/mappers/*.xml"></property>
	</bean>
```

<br/>

