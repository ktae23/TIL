package jpabook.jpashop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JpashopApplication {

	public static void main(String[] args) {
		SpringApplication.run(JpashopApplication.class, args);
	}


//	@Bean
//	Hibernate5Module hibernate5Module() {
//		hibernate5Module().configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
//		return new Hibernate5Module();
//	}
}
