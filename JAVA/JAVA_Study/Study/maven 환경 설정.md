**학습하기**

이번 시간에는 Maven을 이용해 웹 어플리케이션 프로젝트를 생성하고, 실행해보도록 하겠습니다.

이클립스를 실행하고, 이클립스의 메뉴 중 File - New - Project를 선택합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_93/15173903549717lnBe_PNG/maven01.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  프로젝트 위자드(Wizard)가 뜨면, Maven아래의 Maven Project를 선택한 후 Next버튼을 클릭합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_271/1517390405142k98yp_PNG/maven02.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  Maven프로젝트가 기존 워크스페이스 경로에 생성되도록 합니다. Next버튼을 클릭합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_237/15173904247979OJPr_PNG/maven03.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  아키타입(Archetype)을 선택합니다. 아키타입이란 일종의 프로젝트 템플릿(Template)이라고 말할 수 있습니다. 어떤 아키타입을 선택했느냐에 따라서 자동으로, 여러 가지 파일들을 생성하거나 라이브러리를 셋팅해주거나 등의 일을 해줍니다. Maven을 이용하여 웹 어플리케이션을 개발하기 위해서 maven-archetype-webapp를 선택한 후 Next 버튼을 클릭합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_53/151739046174467lID_PNG/maven04.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  Group Id는 보통 프로젝트를 진행하는 회사나 팀의 도메인 이름을 거꾸로 적습니다. Artifact Id는 해당 프로젝트의 이름을 적습니다. 버전은 보통 기본값(0.0.1-SNAPSHOT)으로 설정합니다. package이름은 group id와 Artifact Id가 조합된 이름이 됩니다. Group Id를 kr.or.connect이고 Artifact Id가 mavenweb으로 설정했기 때문에 package이름은 kr.or.connect.mavenweb이 됩니다. finish버튼을 클릭합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_216/1517390501234YqbJ4_PNG/maven05.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  프로젝트가 생성된 프로젝트의 디렉토리 구조입니다. 디렉토리의 구조를 보기 위해서 이클립스의 Navigator view를 통해서 확인하였습니다. Maven으로 생성된 프로젝트의 경우 자바 소스는 src/main/java 폴더에 생성됩니다. 웹 어플리케이션과 관련된 html, css등은 src/main/webapp 폴더에서 작성해야 합니다. 그런데, 생성된 프로젝트를 보면 src/main/java 폴더가 보이지 않습니다. 필요한 폴더는 별도로 만들어줄 필요가 있습니다.

[![img](https://cphinf.pstatic.net/mooc/20180723_93/1532327705849hiuxX_PNG/_.PNG?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

[![img](https://cphinf.pstatic.net/mooc/20180131_41/1517390521930voHdk_PNG/maven06.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

[![img](https://cphinf.pstatic.net/mooc/20180131_252/1517390525001xoFVn_PNG/maven07.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  프로젝트를 선택하고, 우측버튼을 눌러 properties를 선택합니다. 그리고, Java Compiler메뉴를 선택합니다. 선택을 해보면 기본적으로 JDK 1.5 버전을 사용하는 것을 알 수 있습니다. Maven으로 프로젝트를 생성하면 기본적으로 JDK 1.5를 사용하게 됩니다. JDK8을 사용하도록 하려면 Maven설정 파일인 pom.xml파일을 수정해야 합니다. pom.xml파일을 더블클릭하면 다음과 같이 보입니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_125/1517390594478q5Mvw_PNG/maven08.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  아래쪽의 pom.xml 탭을 선택하면 소스가 보입니다.

```markup
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>kr.or.connect</groupId>
  <artifactId>mavenweb</artifactId>
  <packaging>war</packaging>
  <version>0.0.1-SNAPSHOT</version>
  <name>mavenweb Maven Webapp</name>
  <url>http://maven.apache.org</url>
  
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <finalName>mavenweb</finalName>
  </build>
</project>
```

자동으로 juint 3.8.1 라이브러리를 추가하고 있습니다.

junit은 테스트를 위한 라이브러리입니다.

위의 내용에 다음의 코드를 입력합니다.

```markup
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>kr.or.connect</groupId>
  <artifactId>mavenweb</artifactId>
  <packaging>war</packaging>
  <version>0.0.1-SNAPSHOT</version>
  <name>mavenweb Maven Webapp</name>
  <url>http://maven.apache.org</url>

  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <finalName>mavenweb</finalName>
        <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.6.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
    </plugins>
  </build>
</project>
```

코드를 입력하였으면, 저장합니다.

수정 후 다시 프로젝트 프로퍼티의 자바 컴파일러 항목을 보면 여전히 1.5 입니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_152/15173907953931jtNA_PNG/maven09.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  프로젝트 프로퍼티를 선택한 후 Maven메뉴 아래의 Java EE Integration을 선택합니다. 보이는 것처럼 Enable Project Specific Settings 앞의 체크박스를 선택합니다. 그리고 아래의 Apply and Close버튼을 클릭합니다. 그리고, 다시 프로퍼티의 자바 컴파일러 버전을 확인하도록 하겠습니다. JDK 1.8이 사용되는 것을 알 수 있습니다. Maven의 설정을 바꾸면, 이클립스 프로젝트 설정이 연동되게 된 것입니다. 이번엔 webapp폴더 아래의 index.jsp를 열어보도록 하겠습니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_218/1517390838527L7N1g_PNG/maven10.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  HttpServlet을 찾을 수 없다는 오류 메시지가 보입니다. 다음에 배우게 되는 웹 백엔드 프로그래밍 기초 에서는 Dynamic Web Application을 만들 볼텐데요. 그 때는 WAS Runtime설정을 하면서 Tomcat을 지정합니다. Tomcat안에 있는 서블릿 라이브러리가 사용되면서 문제가 없게 됩니다. 실행시에도 WAS 위에서 실행되기 때문에 WAS의 서블릿 라이브러리를 사용하게 됩니다. Maven프로젝트로 생성했을 경우에는 WAS 런타임이 지정을 안 했기 때문에 서블릿 라이브러리를 찾을 수 없습니다. dependencies 엘리먼트 아래에 다음을 추가합니다.

```markup
<dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>
```

위의 내용을 보면 scope에 provided라는 항목이 있는데 servlet라이브러리를 컴파일 시에만 사용되고 배포 시에는 사용되지 않는다는 것을 의미합니다.

scope는 다음과 같은 4가지가 있습니다.

- **compile** : 컴파일 할 때 필요. 테스트 및 런타임에도 클래스 패스에 포함됩니다. scope 을 설정하지 않는 경우 기본값입니다.
- **runtime** : 런타임에 필요. JDBC 드라이버 등이 예가 됩니다. 컴파일 시에는 필요하지 않지만, 실행 시에 필요한 경우입니다.
- **provided** : 컴파일 시에 필요하지만, 실제 런타임 때에는 컨테이너 같은 것에서 제공되는 모듈. servlet, jsp api 등이 이에 해당. 배포 시 제외됩니다. 
- **test** : 테스트 코드를 컴파일 할 때 필요. 테스트 시 클래스 패스에 포함되며, 배포 시 제외됩니다.

위의 내용을 추가하고 index.html을 가보면 오류가 발생하지 않는 것을 알 수 있습니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_230/1517390978066hU3Kl_PNG/maven11.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  해당 프로젝트를 실행해 보도록 하겠습니다. 프로젝트를 선택한 후 우측버튼을 클릭하여 Run on Server를 선택합니다. 해당 웹 어플리케이션을 실행할 Runtime을 지정하고, 항상 해당 런타임을 사용하겠다는 아래쪽 체크박스도 선택한 후 Finish버튼을 클릭합니다.

[![img](https://cphinf.pstatic.net/mooc/20180131_294/1517391005231cu2Kq_PNG/maven12.png?type=w760)](https://www.boostcourse.org/web326/lecture/58938/?isDesc=false#)

- 

  브라우저가 실행되면서 index.html이 보여지는 것을 확인할 수 있습니다. 이번엔 pom.xml 파일에 JSTL라이브러리를 추가하도록 하겠습니다.

```markup
<dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>jstl</artifactId>
        <version>1.2</version>
    </dependency>
```

JSTL은 Tomcat이 기본으로 제공하지 않기 때문에, 컴파일할 때도 배포할 때도 사용돼야 합니다.

그래서 scope에 이번엔 provided가 있지 않습니다.

webapp폴더에 앞에서 작성했던 jstl02.jsp 를 붙여넣기를 하도록 하겠습니다.

라이브러리가 변경되었으니, 다시 run on server를 합니다.

실행해도 결과가 아무것도 나오지 않는 것을 확인할 수 있습니다.

프로젝트 프로퍼티를 선택한 후, Project facets 항목을 보면 다이나믹 웹 모듈의 버전이 2.3입니다.

다이나믹 웹 모듈의 2.4부터 EL이 기본으로 사용할 수 있도록 설정되기 때문에 2.3일 경우에는 EL표기법의 결과가 출력되지 않습니다.

앞의 프로젝트처럼 다이나믹 웹 모듈 3.1이 되도록 설정해보도록 하겠습니다.

먼저 WEB-INF의 web.xml 파일을 열어보도록 하겠습니다.

```markup
<!DOCTYPE web-app PUBLIC
 "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
 "http://java.sun.com/dtd/web-app_2_3.dtd" >

<web-app>
  <display-name>Archetype Created Web Application</display-name>
</web-app>
```

위의 내용을 다음과 같이 수정합니다.

```markup
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd" version="3.1">
  <display-name>Archetype Created Web Application</display-name>
</web-app>
```

프로젝트아래의 .settings/org.eclipse.wst.common.project.facet.core.xml 파일을 엽니다.

Windows > Show veiw > Navigator로 파일을 보면 .settings 파일을 발견할 수 있습니다.

```markup
<?xml version="1.0" encoding="UTF-8"?>
<faceted-project>
  <fixed facet="wst.jsdt.web"/>
  <installed facet="jst.web" version="2.3"/>
  <installed facet="wst.jsdt.web" version="1.0"/>
  <installed facet="java" version="1.8"/>
</faceted-project>
```

을 아래와 같이 수정합니다.

```markup
<?xml version="1.0" encoding="UTF-8"?>
<faceted-project>
  <fixed facet="wst.jsdt.web"/>
  <installed facet="jst.web" version="3.1"/>
  <installed facet="wst.jsdt.web" version="1.0"/>
  <installed facet="java" version="1.8"/>
</faceted-project>
```

프로젝트 프로퍼티의 Project facets항목을 보면 다이나믹 웹 모듈이 3.1로 바뀐 것을 볼 수 있습니다.

이제 jstl02.jsp를 run on server로 실행합니다.

실행을 하지만 오류가 나면서 실행이 안 되는 경우가 있을 수 있습니다.

이클립스의 버그로, 수정되기 전의 데이터와 수정된 데이터가 섞여서 실행되기 때문입니다.

이 경우 웹 어플리케이션을 깔끔히 초기화하고 실행하는 것이 좋을 수 있습니다.

1. 기존 tomcat을 종료합니다.
2. 혹시 바뀌지 않았다면 프로젝트를 선택하고, 우측버튼을 눌러서 Maven 메뉴 아래의 update project를 선택한 후 확인하세요.
3. Servers view에서 기존 Tomcat Runtime을 삭제
4. Project 메뉴의 Clean선택
5. 프로젝트 익스플로러에서 Server 삭제

위와 같은 과정을 거친 후 Run on Server로 실행해보세요.

결과가 잘 나오는 것을 확인할 수 있습니다.

지금까지 배웠던 내용 중에서 가장 복잡한 것 같은데요.

다이나믹 웹 모듈을 2.3에서 3.1로 바꾸는 것은 프로젝트가 한번 만들어지면, 그 이후부터는 그 프로젝트가 더 이상 사용되지 않을 때까지 계속 사용되기 때문에 자주 개발자가 해야 할 일은 아닙니다.

그 이후부터는 pom.xml에 원하는 기능을 추가하면서 개발을 진행하면 됩니다.