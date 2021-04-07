
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.jes.ai.engine;
//네이버 음성합성 Open API 예제
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class TTS {

    public static void process(String longText) {
       String clientId = "uf4hxtdqdo";//애플리케이션 클라이언트 아이디값";
       String clientSecret = "6G2Fc8paabdrpObsRzU6ZNRa3M5mIwWOowDYkiie";//애플리케이션 클라이언트 시크릿값";
       try {

           String text = URLEncoder.encode(longText, "UTF-8"); // 
           String apiURL = "https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts";
           URL url = new URL(apiURL);
           HttpURLConnection con = (HttpURLConnection)url.openConnection();
           con.setRequestMethod("POST");
           con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);//header
           con.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
           // post request
           String postParams = "speaker=nsinu&volume=0&speed=0&pitch=0&format=mp3&text=" + text;           
           con.setDoOutput(true);
           DataOutputStream wr = new DataOutputStream(con.getOutputStream());
           wr.writeBytes(postParams);
           wr.flush();
           wr.close();
           int responseCode = con.getResponseCode();
           BufferedReader br;
           if(responseCode==200) { // 정상 호출
               InputStream is = con.getInputStream();

               int read = 0;
               byte[] bytes = new byte[1024];

               File f = new File("tts.mp3");
               f.createNewFile();
               OutputStream outputStream = new FileOutputStream(f);
               while ((read =is.read(bytes)) != -1) {
                   outputStream.write(bytes, 0, read);
               }
               outputStream.close();
               is.close();
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
           e.printStackTrace();
           System.out.println(e);
       }
    }
}



