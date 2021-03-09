package my.model;

public class PersonImpl implements Person{
	private String name;
	private int age;	

	public PersonImpl( int age, String name) {
		super();
		this.name = name;
		this.age = age;
		System.out.println("생성자(String name, int age) 호출됨");
	}

	public PersonImpl(String name) {
		super();
		this.name = name;
		System.out.println("생성자(String name) 호출됨");
	}

	public PersonImpl() {
		System.out.println("생성자() 호출됨");
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		System.out.println("setName() 호출됨");
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "PersonImpl [name=" + name + ", age=" + age + "]";
	}
	
	
	
	
}