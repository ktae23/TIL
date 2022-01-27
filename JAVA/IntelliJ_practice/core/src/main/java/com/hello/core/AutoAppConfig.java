package com.hello.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        // component scan의 대상 중 제외 설정 필터
        // 기본 값은 현재 클래스의 패키지부터 하위 패키지를 스캔, 해당 클래스를 최상단에 두고 기본 값으로 스캔하는 것을 권장
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Configuration.class)
)
public class AutoAppConfig {


}
