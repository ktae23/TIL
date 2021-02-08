package org.group_a;

public class Test {

	public static void main(String[] args) {
	    MyProfile member_profile = new MyProfile();
	    
		System.out.println(member_profile.age);
		System.out.println(member_profile.tall);
		System.out.println(member_profile.gender);
		System.out.print(member_profile.birthday.year + "년");
		System.out.print(member_profile.birthday.month + "월");
		System.out.print(member_profile.birthday.day + "일");
		System.out.println("");
		System.out.print(member_profile.name.name1);
		System.out.print(member_profile.name.name2);
		System.out.print(member_profile.name.name3);
		
	}
}
