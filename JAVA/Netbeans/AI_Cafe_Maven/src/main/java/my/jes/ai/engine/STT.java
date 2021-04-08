/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.jes.ai.engine;


import com.mulcam.ai.web.vo.ProductVO;
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
import my.jes.ai.CafeUi;
import org.json.JSONObject;

public class STT {

    CafeUi cafeUi;
    
    public STT(CafeUi cafeUi) {
        this.cafeUi=cafeUi;
    }

	public  String process() {
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
					// TODO Auto-generated method stub
					AudioInputStream audioStream=new AudioInputStream(targetDataLine);
					File wavFile=new File("RecordingAudio.wav");
					try {
						AudioSystem.write(audioStream,  AudioFileFormat.Type.WAVE, wavFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			stopper.start();
			Thread.sleep(5000);
			targetDataLine.stop();
                        targetDataLine.close();
			
			
			////////////////////////////
			
			//String clientId = "4fl0k53shk";             // Application Client ID";
	        //String clientSecret = "tFSMyBfocPoGkM1vagG6rBuX1KEiS6ss3vGREN5b";     // Application Client Secret";
			String clientId ="qtjt23f1yd";
			String clientSecret = "GWaTQHkkQmY4ibbiB2hiKvgZAJm39FadacZ1L7sI";
	        try {
	            String imgFile = "RecordingAudio.wav";
	            File voiceFile = new File(imgFile);

	            String language = "Kor";        // 언어 코드 ( Kor, Jpn, Eng, Chn )
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
	                br = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
	            } else {  // 오류 발생
	                System.out.println("error!!!!!!! responseCode= " + responseCode);
	                br = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"));
	            }
	            String inputLine;

	            if(br != null) {
	                StringBuffer response = new StringBuffer();
	                while ((inputLine = br.readLine()) != null) {
	                    response.append(inputLine);
	                }
	                br.close();
	                JSONObject o=new JSONObject(response.toString());
                        String stt=o.getString("text");
                        System.out.println("사용자:"+stt);
                        if(stt.contains("네") || stt.contains("예")){
                            VoiceOrders.flag=true;
                            ProductVO p=VoiceOrders.productList.get(0);
                            cafeUi.basket(p.getProduct_name(),p.getQuantity());
                        }
                        return stt;
	            } else {
	                System.out.println("음성인식 에러 !!!");
                        return "죄송합니다. 다시 말씀해주시겠어요?";
	            }
	        } catch (Exception e) {
	            System.out.println(e);
	        }
            }catch(Exception e) {
                System.out.println(e);
                    //e.printStackTrace();
            }
                return null;
	}

}