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

        try (FileInputStream input = new FileInputStream("///C:/Users/zz238/TestCode.java")) {
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
