package my.chr.ai.test2.voice;

//Cafe2 접속 예제
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public class HttpConTest {

public static void main(String[] args) {
	   try {
	    
	       //URL url = new URL("http://localhost:8090/login.chr"); //연결하고자 하는 url 넣기
	       URL url = new URL("https://openapi.naver.com/v1/datalab/shopping/categories");
	       HttpURLConnection con = (HttpURLConnection)url.openConnection(); //연결 객체 생성
	       
	       con.setRequestMethod("POST"); //GET이나 POST방식으로 요청
	       // post request
	       //String postParams = "id=CHOI&pw=123"; //request message body(여기에 들어가는 것이 parameter)
	       String postParams ="";
	       con.setDoOutput(true);
	       DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	       wr.writeBytes(postParams); //전송
	       //wr.writeBytes(id=CHOI&pw=123); 한번에 가능
	       wr.flush(); //지체하지 않고 전송
	       wr.close(); //전송 끝
	       
	       int responseCode = con.getResponseCode(); //200이면 정상으로 return
	       BufferedReader br;
	       if(responseCode==200) { // 정상 호출
	    	   //txt 받는 코드
	    	   br = new BufferedReader(new InputStreamReader(con.getInputStream()));
	           String inputLine;
	           StringBuffer response = new StringBuffer();
	           while ((inputLine = br.readLine()) != null) {
	               response.append(inputLine);
	           }
	           br.close();
	           System.out.println(response.toString());
	       } else {  // 오류 발생
	    	   //txt 받는 코드
	           br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
	           String inputLine;
	           StringBuffer response = new StringBuffer();
	           while ((inputLine = br.readLine()) != null) {
	               response.append(inputLine);
	           }
	           br.close();
	           System.out.println(response.toString());
	       }
	   } catch (Exception e) {
	       System.out.println(e);
	   }
	}
}