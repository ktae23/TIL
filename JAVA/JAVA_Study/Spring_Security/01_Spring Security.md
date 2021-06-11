## [Spring Security - 환경설정](https://www.youtube.com/watch?v=GEv_hw0VOxE&list=PL93mKxaRDidERCyMaobSLkvSPzYtIk0Ah&index=1)

#### 서버사이드 렌더링 이용 시 뷰리졸버 설정

```java
@Configuration
public class WEbMvcConfig implements WebMvcConfigurer{
    
    @Override
    public void configureViewResolver(ViewResesolverRegitstry registry){
        [사용하는탬플릿엔진리졸버] resolver = new [사용하는탬플릿엔진리졸버];
        resolver.setCharset("UTF-8");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setPrefix("classpath:/templates/");
        resolver.setSeuffix(".html");
        
        regitstry.viewResolver(resolver);
    }
}
```

<br/>

#### SecurityConfig

```java
```

