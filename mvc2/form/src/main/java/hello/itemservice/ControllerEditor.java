package hello.itemservice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ControllerEditor {

    class myTextVo {
        @Override
        public String toString() {
            return "myTextVo{" +
                    "text='" + text + '\'' +
                    '}';
        }

        String text;
    }

    @PostMapping("/submit")
    @ResponseBody
    public String submit(myTextVo myTextVo) {
        System.out.println("myTextVo = " + myTextVo);
        return myTextVo.text;
    }

    @GetMapping("/editor")
    public String submitForm(){
        return "form/editor";
    }
}
