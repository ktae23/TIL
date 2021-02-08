package multiple_choice;

public class Override_Test {
    public String getDetails(){ return "String";}  
    public void getDetails(char c){} 
//    public void getDetails(){}//1   오버라이드 규칙 - 매개변수 데이터 타입 또는 매개변수 갯수가 달라야 함.
    void getDetails(String s){}//2   
    public void getDetails(int i){}//3   
    void getDetails(double d){}//4   
          
}  