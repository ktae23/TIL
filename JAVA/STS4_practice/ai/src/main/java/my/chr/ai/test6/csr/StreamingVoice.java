package my.chr.ai.test6.csr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class StreamingVoice {

	public static void main(String[] args) {
		try {
			AudioFormat format=new AudioFormat(16000,8,2,true,true);
			DataLine.Info info=new DataLine.Info(TargetDataLine.class, format);
			if(!AudioSystem.isLineSupported(info)) {
				System.out.println("Line is not supported");
			}
			
			final TargetDataLine targetDataLine=(TargetDataLine)AudioSystem.getLine(info);
			targetDataLine.open();
			System.out.println("starting Recording while 5sec.");
			targetDataLine.start();
			Thread stopper=new Thread(new Runnable() {
				@Override
				public void run() {
					AudioInputStream audioStream=new AudioInputStream(targetDataLine);
					File wavFile=new File("RecordingAudio.wav");
					try {
						AudioSystem.write(audioStream,  AudioFileFormat.Type.WAVE, wavFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			stopper.start();
			Thread.sleep(5000);
			targetDataLine.stop();
			
			////////////////////////////
			
			//String clientId = "4fl0k53shk";  // Application Client ID";
	        //String clientSecret = "tFSMyBfocPoGkM1vagG6rBuX1KEiS6ss3vGREN5b";  // Application Client Secret";
			String clientId ="qtjt23f1yd";
			String clientSecret = "GWaTQHkkQmY4ibbiB2hiKvgZAJm39FadacZ1L7sI";
	        try {
	            String imgFile = "RecordingAudio.wav";
	            File voiceFile = new File(imgFile);

	            String language = "Kor"; // 언어 코드 ( Kor, Jpn, Eng, Chn )
	            String apiURL = "https://naveropenapi.apigw.ntruss.com/recog/v1/stt?lang=" + language;
	            URL url = new URL(apiURL);

	            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
	            conn.setUseCaches(false);
	            conn.setDoOutput(true);
	            conn.setDoInput(true);
	            conn.setRequestProperty("Content-Type", "application/octet-stream");
	            conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
	            conn.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);

	            OutputStream outputStream = conn.getOutputStream();
	            FileInputStream inputStream = new FileInputStream(voiceFile);
	            byte[] buffer = new byte[4096];
	            int bytesRead = -1;
	            while ((bytesRead = inputStream.read(buffer)) != -1) {
	                outputStream.write(buffer, 0, bytesRead);
	            }
	            outputStream.flush();
	            inputStream.close();
	            BufferedReader br = null;
	            int responseCode = conn.getResponseCode();
	            if(responseCode == 200) { // 정상 호출
	                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            } else {  // 오류 발생
	                System.out.println("error!!!!!!! responseCode= " + responseCode);
	                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            }
	            String inputLine;

	            if(br != null) {
	                StringBuffer response = new StringBuffer();
	                while ((inputLine = br.readLine()) != null) {
	                    response.append(inputLine);
	                }
	                br.close();
	                System.out.println(response.toString());
	            } else {
	                System.out.println("error !!!");
	            }
	        } catch (Exception e) {
	            System.out.println(e);
	        }
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

}