package com.example.monacoeditor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClassTest {
    @GetMapping("/test")
    public String test(Model model){
        String value = (String) model.getAttribute("value");
        System.out.println(value);
        model.addAttribute("value", value);
        return "Test";
    }


}
