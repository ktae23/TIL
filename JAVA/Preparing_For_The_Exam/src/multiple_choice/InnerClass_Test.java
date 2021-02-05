package multiple_choice;

public class InnerClass_Test {
	public static void main(String[] args) {
		
    int out = 10; 
    class Inner{ 
            public int count(){
                    int cnt = 0;
                    for ( int i = 0 ; i < out ; i++){
                            cnt += i;
                    } 
                    return cnt;
            } 
    }  
    Inner in = new Inner();
    System.out.println( in.count() );
		}
	}  

