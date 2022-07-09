


### Rest Assured

`Rest Assured`의 기본 문법은 `BDD` (Behavior Driven Development)와 매우 유사합니다.

```java
Given(). 
        param("x", "y"). 
        header("z", "w").
when().
Method().
Then(). 
        statusCode(XXX).
        body("x, ”y", equalTo("z"));
```	

<BR/>

| Code | 	Explanation|
|--------|-------------| 
| Given()|	요청의 기반 설정을 할 수 있습니다. 요청 헤더, 쿼리 스트링 또는 경로 변수, 리퀘스트 바디 (컨텐트), 쿠키 등을 전달합니다. 이러한 항목이 필요하지 않을 경우 사용하지 않는 선택 사항입니다.|
|When()	|요청을 처리하는 시나리오 전체를 나타냅니다.|
|Method()	|CRUD 작업을 수행하는 HTTP 메서드를 `Method` 자리에 사용합니다.(get/post/put/delete)|
|Then()	|단언문이나 일치 조건을 선언합니다.|

<br/>

학습 테스트를 통해 Rest Assured 사용법을 코드로 확인해보겠습니다.

<br/>

#### maven dependency
##### java 9 미만 버전
```xml
<dependency>
<groupId>io.rest-assured</groupId>
<artifactId>json-path</artifactId>
<version>4.2.0</version>
<scope>test</scope>
</dependency>


<dependency>
<groupId>io.rest-assured</groupId>
<artifactId>xml-path</artifactId>
<version>4.2.0</version>
<scope>test</scope>
</dependency>


<dependency>
<groupId>io.rest-assured</groupId>
<artifactId>json-schema-validator</artifactId>
<version>4.2.0</version>
<scope>test</scope>
</dependency>
```
<br/>

##### java 9이상 버전
```xml
<dependency>
<groupId>io.rest-assured</groupId>
<artifactId>rest-assured-all</artifactId>
<version>4.2.0</version>
<scope>test</scope>
</dependency>
```
<br/>


<hr/>

### 학습 테스트


테스트 코드를 먼저 작성하겠습니다.  <br/>
Rest Assured는 주로 시나리오 기반 통합 테스트 목적으로 사용되기 때문에 테스트 메서드를 모아 둔 클래스를 따로 작성하고 이를 호출하여 결과를 검증하는 테스트를 한 곳에서 수행하는 편입니다.
<br/>

#### TestMain

``` java
	private static final long MEMBER_ID = 1L;
	private static final String SUCCESS_MESSAGE = "test String " + MEMBER_ID;
	
    @Test
    @DisplayName("get method test")
    void getMethodTest() {
        final ExtractableResponse<Response> testString = getTestString(MEMBER_ID);
        Assertions.assertEquals("SUCCESS_MESSAGE , testString.response().body().asString());
        Assertions.assertEquals(HttpStatus.OK.value(), testString.response().statusCode());
    }
```
<br/>

#### TestControllerClass 

```java
class TestControllerClass {
    static ExtractableResponse<Response> getTestString(Long memberId) {
        return given()
			        .log().all()
	                .contentType(MediaType.APPLICATION_JSON_VALUE)
	                .header("Authorization", memberId)
	                .pathParam("memberId", memberId)
                .when()
	                .get("/test/{memberId}")
                .then()
	                .log().all()
                .extract();
    }
}
```
<br/>


테스트 코드를 작성한 뒤 이를 받아줄 컨트롤러를 작성합니다.

#### TestController
```java
@RequestMapping("/test")  
@RestController  
public class TestController {  

  @GetMapping("/{memberId}")  
  public ResponseEntity<String> getTestString(@PathVariable Long memberId) {  
	  return ResponseEntity.ok("test String " + memberId);  
  }  
}
```

그리고 바로 실행해보겠습니다. 결과는 아래와 같습니다.
```
java.net.ConnectException: Connection refused: connect
```

찾아보니 `RestAssured`의 포트를 `랜덤 포트`로 설정하여 넣어주는 설정이 필요하다고 합니다.
아래와 같이 추가하였습니다.

<br/>

#### TestMain

```java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestMain {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }
	//...
}


```
<br/>


변경 뒤 실행하니 초록불이 들어오며 첫 테스트에 성공합니다.

``` bash
Request method:	GET
Request URI:	http://localhost:65358/test/1
Proxy:			<none>
Request params:	<none>
Query params:	<none>
Form params:	<none>
Path params:	memberId=1
Headers:		Authorization=1
				Accept=*/*
				Content-Type=application/json; charset=UTF-8
Cookies:		<none>
Multiparts:		<none>
Body:			<none>
2022-07-09 17:16:44.745  INFO 18468 --- [o-auto-1-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2022-07-09 17:16:44.745  INFO 18468 --- [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2022-07-09 17:16:44.746  INFO 18468 --- [o-auto-1-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 0 ms
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 13
Date: Sat, 09 Jul 2022 08:16:44 GMT
Keep-Alive: timeout=60
Connection: keep-alive

test String 1
```
<hr/>

`Rest Assured`는 `MockMvc`와 달리 대부분 `End to End` 테스트에 사용하며 `@SpringBootTest`로 실제 요청을 보내서 전체적일 로직을 테스트하는데 사용합니다.
<br/>

때문에 순수한` Contoller 레이어`만 분리하여 테스트하는 `MockMvc`와 달리 서비스를 비롯한 연관 된 `스프링 빈`들에 대한 테스트가 함께 수행 됩니다.
<br/>

만일 `Rest Assured`를 이용하면서 `MockMvc`로 테스트 하듯이 레이어 테스트를 원한다면 별도의 설정을 통해 `Controller Layer`만 분리하여 단위 테스트를 할 수 있습니다.


<hr/>

### Rest Assured로 MockMvc 테스트 하기

먼저 MockMvc 사용을 위한 의존성을 추가합니다.
```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>spring-mock-mvc</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```
<br/>
`RestAssuredMockMvc`를 사용하여 테스트를 할때는 어떤 방식으로 실행 할 것인지 선택해야 합니다.
`Stand Alone`과 `Web Application Context` 중 선택할 수 있고, 실행 방식을 테스트 당 한번으로 사용할 수 있습니다.

<hr/>

#### Standalone 방식
한 개 이상의 `@Controller` 또는 `@ControllerAdvice` 어노테이션이 있는 클래스를 이용해 `RestAssuredMockMvc`를 초기화 합니다.

```java
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

    public static ExtractableResponse<Response> getTestString(Long memberId) {
        return given().standaloneSetup(new TestController()) 
        // 또는 TestMain에서 @Autowired 후 파라미터로 전달 받아 사용
```	

또는 여러 테스트를 수행하는 경우에는 미리 설정을 해둘 수 있습니다.

```java
    @BeforeEach
    void portSetUp() {
        RestAssured.port = port;
        RestAssuredMockMvc.standaloneSetup(new TestController());
    }
```

standaloneSetup()의 시그니처는 다음과 같습니다. 가변 인자를 사용하기 때문에 하나 이상의 대상을 사용할 수 있습니다.

```java
    public static void standaloneSetup(Object... controllersOrMockMvcConfigurers) {
        mockMvcFactory = StandaloneMockMvcFactory.of(controllersOrMockMvcConfigurers);
    }
```

<hr/>

#### Web Application Context 방식
spring 의 WebApplicationContext을 이용하는 방식입니다. standaloneSetup()과 유사한 방식으로 테스트 마다 초기화 할 수 있습니다.

TestMain에서 WebApplicationContext를 주입 받아 인자로 넘겨줍니다.

```java
// TestMain

	@Autowired
    private WebApplicationContext webApplicationContext;
// ===================================
// TestContreollerClass

    public static ExtractableResponse<MockMvcResponse> getTestString(Long memberId, WebApplicationContext context) {
        return given().webAppContextSetup(context)
	                .log().all()
	                .contentType(MediaType.APPLICATION_JSON_VALUE)
	                .header("Authorization", memberId)
                .when()
	                .get("/test/{memberId}", memberId)
                .then()
	                .log().all()
                .extract();
    }


```

또는 여러 테스트를 위해 미리 설정해둘 수 있습니다.
```java
    @BeforeEach
    void portSetUp() {
        RestAssured.port = port;
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
     }
```

<hr/>

`Rest Assured`를 사용해 `Controller Unit Test`를 하기 위한 `Mock` 설정 방법을 배웠습니다.
이제 아래와 같이 `standaloneSetup()`과 `mock 주입`을 합니다.

`tandaloneSetup` 방식은 `사용자가 제공한 mock`만을 이용해서 초기화하기 때문에 `유닛 테스트 목적으로 적합`합니다.

그리고 이외에 테스트에 필요한 여러 클래스를 추가합니다.

<br/>

#### BadRequestException (for test)

```java
package com.example.restassuredstudy;  
  
public class BadRequestException extends RuntimeException {  
  public BadRequestException() {  
	  super();  
  }  
}
```

<br/>

#### TestControllerExceptionHandler

```java
package com.example.restassuredstudy;  
  
import org.springframework.http.HttpStatus;  
import org.springframework.web.bind.annotation.ExceptionHandler;  
import org.springframework.web.bind.annotation.ResponseStatus;  
import org.springframework.web.bind.annotation.RestControllerAdvice;  
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;  
  
@RestControllerAdvice(assignableTypes = TestController.class)  
public class TestControllerExceptionHandler extends ResponseEntityExceptionHandler {  
  
  @ResponseStatus(HttpStatus.BAD_REQUEST)  
  @ExceptionHandler(BadRequestException.class)  
  private void badRequest(BadRequestException badRequestException){  
 }}
```

<br/>

#### TestService

```java
package com.example.restassuredstudy;  
  
import org.springframework.stereotype.Service;  
  
@Service  
public class TestService {  
  
	public String getTestString(Long memberId) {  
	if (1L == memberId) {  
		return "test String " + memberId;  
	}  
	throw new BadRequestException();  
	}  
}
```
<br/>

그리고 실패 테스트 케이스와 다른 방식의 테스트 코드를 추가합니다.

처음에 Rest Assured가 시나리오 기반의 통합/인수 테스트를 위해 사용되는 방식에 대해 보여드렸는데요.
이제는 컨트롤러 유닛 테스트를 진행하기 때문에 테스트 검증을 바로 수행하는 케이스를 추가했습니다.

<br/>

#### TestMain

```java
package com.example.restassuredstudy;  
  
import io.restassured.module.mockmvc.RestAssuredMockMvc;  
import io.restassured.module.mockmvc.response.MockMvcResponse;  
import io.restassured.response.ExtractableResponse;  
import joptsimple.internal.Strings;  
import org.hamcrest.Matchers;  
import org.junit.jupiter.api.Assertions;  
import org.junit.jupiter.api.BeforeEach;  
import org.junit.jupiter.api.DisplayName;  
import org.junit.jupiter.api.Test;  
import org.junit.jupiter.api.extension.ExtendWith;  
import org.mockito.InjectMocks;  
import org.mockito.Mock;  
import org.mockito.Mockito;  
import org.mockito.junit.jupiter.MockitoExtension;  
import org.springframework.http.HttpStatus;  
import org.springframework.http.MediaType;  
  
import static com.example.restassuredstudy.TestControllerClass.getTestString;  
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;  
import static org.hamcrest.Matchers.equalTo;  
import static org.hamcrest.core.Is.is;  
  
@ExtendWith(MockitoExtension.class)  
class TestMain {  
  
	@Mock  
	private TestService testService;  
	@InjectMocks  
	private TestController testController;  
	@InjectMocks  
	private TestControllerExceptionHandler testControllerExceptionHandler;  

	private static final long MEMBER_ID = 1L;  
	private static final long FAIL_MEMBER_ID = 2L;  
	private static final String SUCCESS_MESSAGE = "test String " + MEMBER_ID;  
  
	@BeforeEach  
	void setUp() {  
		RestAssuredMockMvc.standaloneSetup(testController, testControllerExceptionHandler);  
	}  
	
	@Test  
	@DisplayName("get method test")  
	void getMethodTest() {  
		Mockito.when(testService.getTestString(MEMBER_ID)).thenReturn(SUCCESS_MESSAGE);  

		final ExtractableResponse<MockMvcResponse> testString = getTestString(MEMBER_ID);  

		Assertions.assertEquals(SUCCESS_MESSAGE, testString.response().body().asString());  
		Assertions.assertEquals(HttpStatus.OK.value(), testString.response().statusCode());  
	}  
	
	@Test  
	@DisplayName("get method test another version")  
	void getMethodTestAnotherVersion() {  
		Mockito.when(testService.getTestString(MEMBER_ID)).thenReturn(SUCCESS_MESSAGE);  

		given()  
			.log().all()  
			.contentType(MediaType.APPLICATION_JSON_VALUE)  
			.header("Authorization", MEMBER_ID)  
		.when()  
			.get("/test/{memberId}", MEMBER_ID)  
		.then()  
			.log().all()  
		.assertThat()  
			.statusCode(HttpStatus.OK.value())  
			.body(is(equalTo(SUCCESS_MESSAGE)));  
	}  
  
	@Test  
	@DisplayName("get method fail test")  
	void getMethodFailTest() {  
		Mockito.when(testService.getTestString(FAIL_MEMBER_ID)).thenThrow(new BadRequestException());  

		final ExtractableResponse<MockMvcResponse> testString = getTestString(FAIL_MEMBER_ID);  

		Assertions.assertTrue(Strings.EMPTY.equals(testString.response().body().asString()));  
		Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), testString.response().statusCode());  
	}  
  
	@Test  
	@DisplayName("get method fail test another version")  
	void getMethodFailTestAnotherVersion() {  
		Mockito.when(testService.getTestString(FAIL_MEMBER_ID)).thenThrow(new BadRequestException());  

		given()  
			.log().all()  
			.contentType(MediaType.APPLICATION_JSON_VALUE)  
			.header("Authorization", FAIL_MEMBER_ID)  
		.when()  
			.get("/test/{memberId}", FAIL_MEMBER_ID)  
		.then()  
			.log().ifValidationFails()  
		.assertThat()  
			.statusCode(HttpStatus.BAD_REQUEST.value())  
			.body(Matchers.emptyString());  
	}  
}

```
<br/>

#### TestControllerClass 

```java
package com.example.restassuredstudy;  
  
import io.restassured.module.mockmvc.response.MockMvcResponse;  
import io.restassured.response.ExtractableResponse;  
import org.springframework.http.MediaType;  
  
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;  
  
  
class TestControllerClass {  
  
  public static ExtractableResponse<MockMvcResponse> getTestString(Long memberId) {  
  return  given()  
			.log().all()  
			.contentType(MediaType.APPLICATION_JSON_VALUE)  
			.header("Authorization", memberId)  
		.when()  
			.get("/test/{memberId}", memberId)  
		.then()  
			.log().all()  
		.extract();  
	}  
}
```
<br/>

이렇게 준비한 테스트를 수행하면 결과는 초록불이 뜨며 아래와 같이 로그가 출력 됩니다.
이러한 방법으로 `Mock`과 `Rest Assured`를 이용하여 `Controller Unit Test`를 수행할 수 있습니다.
<br/>


#### 테스트 결과
```bash
19:34:38.066 [main] DEBUG _org.springframework.web.servlet.HandlerMapping.Mappings - 
	c.e.r.TestController:
	{GET [/test/{memberId}]}: getTestString(Long)
19:34:38.066 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping - 1 mappings in org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
19:34:38.070 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter - ControllerAdvice beans: 0 @ModelAttribute, 0 @InitBinder, 1 RequestBodyAdvice, 1 ResponseBodyAdvice
19:34:38.073 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver - ControllerAdvice beans: 1 @ExceptionHandler, 1 ResponseBodyAdvice
19:34:38.073 [main] INFO org.springframework.mock.web.MockServletContext - Initializing Spring TestDispatcherServlet ''
19:34:38.073 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet - Initializing Servlet ''
19:34:38.073 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - Detected AcceptHeaderLocaleResolver
19:34:38.073 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - Detected FixedThemeResolver
19:34:38.073 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - Detected org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator@e7ecd
19:34:38.073 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - Detected org.springframework.web.servlet.support.SessionFlashMapManager@e3658c
19:34:38.073 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - enableLoggingRequestDetails='false': request parameters and headers will be masked to prevent unsafe logging of potentially sensitive data
19:34:38.073 [main] INFO org.springframework.test.web.servlet.TestDispatcherServlet - Completed initialization in 0 ms
Request method:	GET
Request URI:	http://localhost:8080/test/1
Proxy:			<none>
Request params:	<none>
Query params:	<none>
Form params:	<none>
Path params:	<none>
Headers:		Content-Type=application/json
				Authorization=1
Cookies:		<none>
Multiparts:		<none>
Body:			<none>
19:34:38.076 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - GET "/test/1", parameters={}
19:34:38.076 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping - Mapped to com.example.restassuredstudy.TestController#getTestString(Long)
19:34:38.077 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor - Using 'text/plain', given [*/*] and supported [text/plain, */*, application/json, application/*+json]
19:34:38.077 [main] DEBUG org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor - Writing ["test String 1"]
19:34:38.077 [main] DEBUG org.springframework.test.web.servlet.TestDispatcherServlet - Completed 200 OK
200
Content-Type: text/plain;charset=ISO-8859-1
Content-Length: 13

test String 1

```


- [출처 및 참고]
	- https://www.guru99.com/rest-assured.html
	- https://www.baeldung.com/rest-assured-tutorial
	- https://www.baeldung.com/spring-mock-mvc-rest-assured
