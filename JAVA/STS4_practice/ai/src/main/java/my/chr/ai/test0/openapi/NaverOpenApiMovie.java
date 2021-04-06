package my.chr.ai.test0.openapi;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NaverOpenApiMovie {

    public static void main(String[] args) {
    	try {

			String text = URLEncoder.encode("인터스텔라", "UTF-8");
			URL url = new URL("https://openapi.naver.com/v1/search/movie.json?query="+text);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("X-Naver-Client-Id", "Nfnslmx0ttb_WIQ4rdyu");
			con.setRequestProperty("X-Naver-Client-Secret", "Pnrn1YcSHn");

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
