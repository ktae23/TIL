package coupling.test;

import java.io.IOException;

public class Tester {
   public static void main(String args[]) throws IOException {
      Show b = new B();
      Show c = new C();

      A a = new A(b);          
      //a.display()는  'A'와 'B'를 출력 한다.    
      a.display();

      A a1 = new A(c);
      //a1.display()는  'A'와 'C'를 출력 한다.      
      a1.display();
   }
}

interface Show {
   public void display();
}

class A {
   Show s;
   public A(Show s) {
      //s는 A에 느슨한 결합이 되어 있다.
      this.s = s;
   }

   public void display() {
      System.out.println("A");
      s.display();
   }
}

class B implements Show {    
   public B(){}
   public void display() {
      System.out.println("B");
   }
}

class C implements Show {    
   public C(){}
   public void display() {
      System.out.println("C");
   }
}