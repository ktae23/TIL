
How to add "Monaco-editor.js" that reads and uses your files in Spring project with thymeleaf(SSR)



[모나코 에디터](https://microsoft.github.io/monaco-editor/playground.html#creating-the-editor-editor-basic-options)는 VSCode 기반으로 만들어진 온라인 코드 에디터입니다.

여러 사용 방법이 있기 때문에 다양하게 활용이 가능한 에디터입니다.

링크를 참고하시어 사용 방법을 활용해보세요.



**1. install monaco-editor in static directory using npm.**

1. 먼저 모나코 에디터를 설치합니다. 저는 npm을 사용합니다. static 폴더 밑에 npm install 해주세요.

![img](https://blog.kakaocdn.net/dn/cIfx4C/btra6HhJBU1/OSIhcPMZiowjU4mBKWDtu0/img.png)![img](https://blog.kakaocdn.net/dn/NKVn8/btraQasEXHu/zDcWrrxQJTPYaqzShw5pK0/img.png)

**2. install jquery in static directory using npm.**

- 제이쿼리도 마찬가지로 설치해주세요. 
- 모나코 에디터에는 여러 폴더가 설치 되는데 이 중 min 버전이 바로 사용 가능하도록 제공하는 버전입니다.

![img](https://blog.kakaocdn.net/dn/JFTNo/btra09Mv4lq/FnmqMcJ1BTxzbzmgDtWga0/img.png)



**3. make HTML file for print monaco-editor.**

- 모나코 에디터를 사용할 곳에 헤더 태그 내의 링크들을 순서대로 모두 추가해주세요.
  - require 선언 부의 vs 경로 부분은 상대경로로 잡아주세요.
- 타임리프 변수를 사용하려다 보니 th:inline태그로 body 태그 안에 넣었습니다. 꼭 이럴 필요는 없습니다.
- setEditor()는 출력 값과 언어를 변수로 받아 설정해주기 위해 제가 임의 밖으로 빼서 넣었습니다.
  - 기본 적으로 monaco.editor.create()의 첫번째 파라미터가 출력값, 두번째 파라미터가 설정값입니다.

```
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>모나코 에디터 연습</title>
    <link rel="stylesheet" data-name="vs/editor/editor.main"
          th:href="@{/static/node_modules/monaco-editor/min/vs/editor/editor.main.css}"/>
    <script th:src="@{/static/node_modules/jquery/dist/jquery.js}"></script>

    <script>var require = {paths: {'vs': '../node_modules/monaco-editor/min/vs'}};</script>
    <script th:src="@{/static/node_modules/monaco-editor/min/vs/loader.js}"></script>
    <script th:src="@{/static/node_modules/monaco-editor/min/vs/editor/editor.main.nls.js}"></script>
    <script th:src="@{/static/node_modules/monaco-editor/min/vs/editor/editor.main.js}"></script>

</head>
<body>

<div>
    <h2>온라인 코드 에디터</h2>
    <pre id="monaco"></pre>
</div>

<script th:inline="javascript">
    function setEditor(inputValue, inputLanguage){
        // Monaco editor 옵션 참고 : https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditoroptions.html
        return {
            value: inputValue,
            language: inputLanguage,    // 언어
            theme: "vs-dark",   // 테마
            lineNumbers: 'on',  // 줄 번호
            glyphMargin: false, // 체크 이미지 넣을 공간이 생김
            vertical: 'auto',
            horizontal: 'auto',
            verticalScrollbarSize: 10,
            horizontalScrollbarSize: 10,
            scrollBeyondLastLine: false, // 에디터상에서 스크롤이 가능하게
            readOnly: true,    // 수정 가능 여부
            automaticLayout: true, // 부모 div 크기에 맞춰서 자동으로 editor 크기 맞춰줌
            minimap: {
                enabled: false // 우측 스크롤 미니맵
            },
            lineHeight: 19
        }
    }
    /*<![CDATA[*/
    const monaco_test = new monaco.editor.create(document.getElementById('monaco'), setEditor([[${value}]], [[${language}]]));
    $('#monaco').height((monaco_test.getModel().getLineCount() * 19) + 10); // 19 = 줄 높이, 10 = 세로 스크롤 높이
    /*]]>*/
</script>
</body>
</html>
```







**4. make WebMvcConfig and add resourceHndler to registry for use static resources.**

- static 폴더에 있는 정적 자원을 가져와 사용하기 위한 설정을 추가해 줍니다.

```
package com.example.monacoeditor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(20);
    }

}
```



**5. bring your code and print it!**

- 이제 코드를 가져와 출력하는 코드를 짭니다.

```
package com.example.monacoeditor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

@Controller
public class CodeController {

    @GetMapping("/")
    public String edit(Model model){
        model.addAttribute("value", getFile());
        model.addAttribute("language", "java");
        return "monaco";
    }

    public String getFile() {

        String code = "";

        try (FileInputStream input = new FileInputStream("///C:/file/path/TestCode.java")) {
            InputStreamReader reader = new InputStreamReader(input, "utf-8");
            BufferedReader in = new BufferedReader(reader);

            int ch;
            while ((ch = in.read()) != -1) {
                code += (char) ch;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }
}
```



**6. result**

- 결과
- 예시로 사용한 코드는 위에 있는 컨트롤러 코드를 그대로 사용했습니다.

![img](https://blog.kakaocdn.net/dn/cj8ICP/btraVUiFxhj/NKYpPU2l6vqESrKkX1vORK/img.png)

- 미니맵과 읽기전용 설정을 변경하면 아래와 같이 사용할 수 있습니다.

![img](https://blog.kakaocdn.net/dn/buk9lc/btraXW1lqoC/0POwZzqZs024RwZUjZwzHk/img.png)![img](https://blog.kakaocdn.net/dn/bq8a71/btraVUwal71/YOZ8m4wnFSkp63dsUHD4lk/img.png)

- 기본적으로 VS Code이기 때문에 VS Code의 많은 부분을 사용 할 수있습니다.



이상으로 프론트엔드를 분리하지 않은 상태에서 모나코 에디터를 추가해 사용하는 방법을 알아봤습니다.

저는 이 방법을 [[참고 영상\]](https://youtu.be/aKtH4o5-5UY)을 보고 알게 되었습니다. 아주 훌륭한 영상입니다.

영상에서는 블로그에서 코드를 보여주기 위해 pre 태그 내에 코드를 작성해두고 가져와 사용하는 방법을 알려줍니다.

![img](https://scrap.kakaocdn.net/dn/IVuVK/hyK4TnGYiS/FV78437GXQYe2DpFwiBeN0/img.jpg?width=1280&height=720&face=0_0_1280_720)