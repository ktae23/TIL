package my.pkt.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {
	
	@ResponseBody
	@RequestMapping("/home")
	public String home() {
		System.out.println("Hello Boot~!");
		return "Hello Boot! ㅂㅂㅂ";
	}

}
