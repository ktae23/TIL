package test.di;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

import my.model.Person;
import my.service.MemberService;

public class TestMember {

	public static void main(String[] args) {
		
		BeanFactory factory=new XmlBeanFactory(new FileSystemResource("member.xml"));
		MemberService service=(MemberService) factory.getBean("memberServiece");
		service.listMembers();
	
	}
}
