package my.pkt.ai.test2.voice;

//네이버 음성합성 Open API 예제
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

/**
 * 1. maven project 아니어도 됨
 * 2. 제공받은      String clientId 와 clientSecret 값을 넣고 바로 실행한뒤 이 프로젝트를 리프레쉬하면 임의숫자.mp3가 보임
 * 3. 이 파일을 오디오 플레이어로 실행해 보면 '만나서 반갑습니다'라고 나옴
 * 4. 더 긴 텍스트를 읽어서 처리하도록 응용하기
 * @author javan_000
 *
 */
public class HttpConTest {

 public static void main(String[] args) {
     try {

         URL url = new URL("http://localhost:8080/login");
         HttpURLConnection con = (HttpURLConnection)url.openConnection();
         con.setRequestMethod("POST");
        
         // post request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes("id=a&pw=b");
         wr.flush();
         wr.close();
         
         int responseCode = con.getResponseCode();
         BufferedReader br;
         if(responseCode==200) { // 정상 호출
        	 br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        	 String inputLine;
        	 StringBuffer response = new StringBuffer();
             while ((inputLine =br.readLine()) != null) {
            	 response.append(inputLine);
             }
             br.close();
             System.out.println(response.toString());
         } else {  // 오류 발생
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