package tts;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
public class TtsTest {
	
    public static String file_reader(){
        Path path = Paths.get("Sample.txt");
        Charset cs = StandardCharsets.UTF_8;
        List<String> list = new ArrayList<String>();
        try{
            list = Files.readAllLines(path,cs);
        }catch(IOException e){
            e.printStackTrace();
        }
        for(String readLine : list){
            System.out.println(readLine);
        }
		return list.toString();


    }


	
	
	
public static void main(String[] args) {
	String SttText = file_reader();
    String clientId = "uf4hxtdqdo";
    String clientSecret = "6G2Fc8paabdrpObsRzU6ZNRa3M5mIwWOowDYkiie";
	   try {
	       String longText = URLEncoder.encode(SttText, "UTF-8"); 
	       String apiURL = "https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts";
	       URL url = new URL(apiURL); 
	       HttpURLConnection con = (HttpURLConnection)url.openConnection(); 
	       con.setRequestMethod("POST"); 
	       con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
	       con.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret); 

	       String postParams = "speaker=nara&volume=0&speed=0&pitch=0&format=mp3&text=" + longText; 
	       con.setDoOutput(true);
	       DataOutputStream wr = new DataOutputStream(con.getOutputStream());
	       wr.writeBytes(postParams);
	       wr.flush();
	       wr.close();
	       int responseCode = con.getResponseCode();
	       BufferedReader br;
	       if(responseCode==200) {
	           InputStream is = con.getInputStream();
	           int read = 0;
	           byte[] bytes = new byte[1024];
	           File f = new File("sample2.mp3");
	           f.createNewFile();
	           OutputStream outputStream = new FileOutputStream(f);
	           while ((read =is.read(bytes)) != -1) {
	               outputStream.write(bytes, 0, read);
	           }
	           is.close();
	       } else {  
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