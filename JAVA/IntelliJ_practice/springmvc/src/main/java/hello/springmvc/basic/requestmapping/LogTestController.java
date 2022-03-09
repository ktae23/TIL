package hello.springmvc.basic.requestmapping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LogTestController {

//    @Slf4j lombok
//    private final Logger log = LoggerFactory.getLogger(getClass());

    @GetMapping("/log-test")
    public String logTest() {
        String name = "Spring";

//        더하기 연산을하면 미리 계산하여 메모리에 올라가기 때문에 메서드 인자로 넘겨야 함.
//        log.trace(" info trace="+ name);
        log.trace(" info trace={}", name);
        log.debug(" info debug={}", name);
        log.info(" info log={}", name);
        log.warn(" info warn={}", name);
        log.error(" info error={}", name);
        return "ok";
    }
}
