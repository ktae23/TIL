package test.di;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

import my.model.Person;

public class Test {

	public static void main(String[] args) {
		// PesonImpl person1 = new PersonImpl();
		// Person1.setName("박경태")
		
		BeanFactory factory=new XmlBeanFactory(new FileSystemResource("person.xml"));
		Person p1=(Person) factory.getBean("person1");
		System.out.println(p1);
	
	}
}
