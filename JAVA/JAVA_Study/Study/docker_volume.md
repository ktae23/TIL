### 프로젝트 외부 경로 파일 가져오기

#### 외부 경로를 설정하기 위한 WebMvcConfig.java

```java
package com.proto.mm.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	
    @Value("${resources.location}")
    private String resourcesLocation;
    @Value("${resources.uri_path:}")
    private String resourcesUriPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(resourcesUriPath + "/**")
                .addResourceLocations("file://" + resourcesLocation);

    }

}
```

<br/>

#### application.properties

```properties
# poster image path

#window
resources.location:/C:/Users/user/
resources.uri_path=/poster

#linux
resources.location:/
resources.uri_path=/poster
```

<br/>

- poster로 매핑되는 패스에  resources.location이 적용 됨
  - /poster/{filePath}
  - == file:///C:/Users/user/MM/movie_imgs/[파일이름].png

#### filepath

```properties
MM/movie_imgs/[파일이름].png
```

<br/>

#### Docker 이용 시 볼륨 설정 옵션

```shell
sudo docker run -v ~/[실제 linux 상 파일 경로]:[도커 컨테이너 경로]
# 실제 코드
sudo docker run -v ~/Final/git/MM/movie_imgs:/MM/movie_imgs
```

- 컨데이너 상 경로는 프로젝트가 root directory인 `/`부터 시작해야 함

```shell
ubuntu@MovieMentor:~/Final/git/MM/MM_ProtoType$ sudo docker run --name mm -d -v ~/Final/git/MM/movie_imgs:/MM/movie_imgs -p [port]:[port] [container name]
c4c300712eb65e5d01d5553b2a596e3a215fc512c547d97846d1ece33a65cd33
# --name 은 도커 이미지에 이름 붙이기
# -d는 뒤에서 실행 시키는 옵션
```

![](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.05.17/ubuntu_path.png?raw=true)

- 위의 우분투 경로 상의 사진들이 도커 이미지 실행 후 아래의 컨테이너상 경로에도 동일하게 보인다

![](https://github.com/ktae23/TIL/blob/master/JAVA/JAVA_Study/Study/imgs/2021.05.17/container_path.png?raw=true)

<br/>

이외 자주 사용한 도커 명령어들

```  shell
# jar 또는 war와 dockerfile이 있는 곳에서/ 맨 뒤 한칸 띄우고 . 까지
docker build -t [도커 이미지 이름(태그)] .

# 실행중인 이미지 확인
docker ps

# 전체 이미지 확인
docker images

# 실행중인 이미지 제거
docker rmi -f [image ID]

# 실행 시 별도로 이름 정했을 경우 이미지 재시작
restart [컨테이너 이름]

# 실행 시 별도로 이름 정했을 경우 이미지 제거
rm [컨테이너 이름]
```

