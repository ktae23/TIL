package my.chr.ai.test4.ocr;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class OCRGeneralAPIDemo {

	public static void main(String[] args) {
		String apiURL = "https://71d215e8c41a403d87510748a25daa51.apigw.ntruss.com/custom/v1/6492/cb74be10737afe8cdc992beb83a3e67e3ae317cc0ca43f809606345088150cc9/general";
		String secretKey = "cFJMRFRKSUZTZWZVaWl0VXlvYU5lYlpISmF6aEtpYVI=";
		String imageFile = "car_no.png"; //152가 3108

		try {
			URL url = new URL(apiURL);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setReadTimeout(30000); //30초 후에도 데이터가 안오면 강제종료
			con.setRequestMethod("POST");
			String boundary = "----" + UUID.randomUUID().toString().replaceAll("-", "");
			con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); //header에 넣는 값
			con.setRequestProperty("X-OCR-SECRET", secretKey); //header에 넣는 값

			JSONObject json = new JSONObject();
			json.put("version", "V2");
			json.put("requestId", UUID.randomUUID().toString()); //randomUUID로 랜덤한 userID값 사용
			json.put("timestamp", System.currentTimeMillis());
			JSONObject image = new JSONObject();
			image.put("format", "png");
			image.put("name", "test2");
			JSONArray images = new JSONArray();
			images.put(image);
			json.put("images", images);
			String postParams = json.toString();

			con.connect();
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			long start = System.currentTimeMillis();
			File file = new File(imageFile);
			//이미지 파일은 text가 아니므로 바로 http로 보낼 수 없다
			//따라서 따로 처리하여 보낸다
			writeMultiPart(wr, postParams, file, boundary);
			wr.close();

			int responseCode = con.getResponseCode();
			BufferedReader br;
			if (responseCode == 200) {
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else {
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}
			br.close();

			System.out.println(response);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static void writeMultiPart(OutputStream out, String jsonMessage, File file, String boundary) throws
		IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("--").append(boundary).append("\r\n");
		sb.append("Content-Disposition:form-data; name=\"message\"\r\n\r\n");
		sb.append(jsonMessage);
		sb.append("\r\n");

		out.write(sb.toString().getBytes("UTF-8"));
		out.flush();

		if (file != null && file.isFile()) {
			out.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
			StringBuilder fileString = new StringBuilder();
			fileString
				.append("Content-Disposition:form-data; name=\"file\"; filename=");
			fileString.append("\"" + file.getName() + "\"\r\n");
			fileString.append("Content-Type: application/octet-stream\r\n\r\n");
			out.write(fileString.toString().getBytes("UTF-8"));
			out.flush();
			
			//파일을 읽어서 OutputStream에 write
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] buffer = new byte[8192];
				int count;
				while ((count = fis.read(buffer)) != -1) {
					out.write(buffer, 0, count);
				}
				out.write("\r\n".getBytes());
			}

			out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
		}
		out.flush();
	}
}