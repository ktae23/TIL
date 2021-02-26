package practice;

public class Children extends Parents {
	
    
    String myname = "Kyungtae";
	
	public Children () {
		super();
		System.out.println("자식 소환");
	}

	public Children (int money, String familyname, String myname) {
		super(money, familyname);
		this.myname=myname;
		System.out.println("자식 소환");
	}

	public String showChildrenInfo() {
		return "부모님의 정보는 : " + super.showParentsInfo() + "\n저의 이름는 :" + familyname + "" + myname + "\n가진 돈은 :" + money;
	}
	
	
	
	public void Study() {
		System.out.println("Nope! Play Hard");
	}

	public static void main(String[] args) {
     	Children child = new Children(27000,"김","수한무거북이와두루미");
     	Parents child2= new Children();
     	
     	child2.Study();
			
		
		System.out.println("물려받은 재산은 " + child.money);
		System.out.println("물려받은 이름은 " + child.familyname);
		System.out.println("내 이름은 " + child.myname);
		child.Study();
		
		System.out.println("==========================");

		System.out.println(child.showChildrenInfo());
	}
}
