package my.chr.ai.test0.openapi;

//Cafe2 접속 예제
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public class KakaoOpenApiSearch {

public static void main(String[] args) {
		try {

			String text = URLEncoder.encode("이효리", "UTF-8");
			URL url = new URL("https://dapi.kakao.com/v2/search/web?query="+text);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "KakaoAK a36b5dcb50149087a22279453d2e2c88");
		    
			/*
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			
			wr.writeBytes(text);// 전송
			wr.flush();
			wr.close();*/

			int responseCode = con.getResponseCode();
			System.out.println(responseCode);
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = br.readLine()) != null) {
					response.append(inputLine);
				}
				br.close();
				System.out.println(response.toString());
			} else { // 오류 발생
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