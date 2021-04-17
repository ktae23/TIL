package stt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.google.gson.Gson;

public class SttTest {

	private static final String SECRET = "73c82a01365a42de8506f3968af9f01a";
	private static final String INVOKE_URL = "https://clovaspeech-gw.ncloud.com/external/v1/436/72a56953b0899ab8c0d03719ac1de58b72238fd4e5a7ef28bddac4cb4f36b020";
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	private Gson gson = new Gson();

	private static final Header[] HEADERS = new Header[] {
		new BasicHeader("Accept", "application/json"),
		new BasicHeader("X-CLOVASPEECH-API-KEY", SECRET),
	};

	public static class Boosting {
		private String words;

		public String getWords() {
			return words;
		}

		public void setWords(String words) {
			this.words = words;
		}
	}

	public String url(String url, String completion, String callback, Map<String, Object> userdata,
		String forbiddens, List<Boosting> boostings) {
		HttpPost httpPost = new HttpPost(INVOKE_URL + "/recognizer/url");
		httpPost.setHeaders(HEADERS);
		Map<String, Object> body = new HashMap<>();
		body.put("url", url);
		body.put("language", "ko-KR");
		body.put("completion", completion);
		body.put("callback", callback);
		body.put("userdata", userdata);
		body.put("forbiddens", forbiddens);
		body.put("boostings", boostings);
		HttpEntity httpEntity = new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		return execute(httpPost);
	}

	public String objectStorage(String dataKey, String completion, String callback, Map<String, Object> userdata,
		String forbiddens, List<Boosting> boostings) {
		HttpPost httpPost = new HttpPost(INVOKE_URL + "/recognizer/object-storage");
		httpPost.setHeaders(HEADERS);
		Map<String, Object> body = new HashMap<>();
		body.put("dataKey", dataKey);
		body.put("language", "ko-KR");
		body.put("callback", callback);
		body.put("completion", completion);
		body.put("userdata", userdata);
		body.put("forbiddens", forbiddens);
		body.put("boostings", boostings);
		StringEntity httpEntity = new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON);
		httpPost.setEntity(httpEntity);
		return execute(httpPost);
	}

	public String upload(File file, String completion, String callback, Map<String, Object> userdata,
		String forbiddens, List<Boosting> boostings) {
		HttpPost httpPost = new HttpPost(INVOKE_URL + "/recognizer/upload");
		httpPost.setHeaders(HEADERS);
		Map<String, Object> body = new HashMap<>();
		body.put("language", "ko-KR");
		body.put("callback", callback);
		body.put("completion", completion);
		body.put("userdata", userdata);
		body.put("forbiddens", forbiddens);
		body.put("boostings", boostings);
		HttpEntity httpEntity = MultipartEntityBuilder.create()
			.addTextBody("params", gson.toJson(body), ContentType.APPLICATION_JSON)
			.addBinaryBody("media", file, ContentType.MULTIPART_FORM_DATA, file.getName())
			.build();
		httpPost.setEntity(httpEntity);
		return execute(httpPost);
	}

	private String execute(HttpPost httpPost) {
		try (final CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
			final HttpEntity entity = httpResponse.getEntity();
			return EntityUtils.toString(entity, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	//직접 녹음을 할 경우 사용.
	 public static void voiceRecord() { 
		 
		 try {
			 AudioFormat format=new
			 AudioFormat(16000,8,2,true,true); DataLine.Info info=new
			 DataLine.Info(TargetDataLine.class, format);
			 if(!AudioSystem.isLineSupported(info)) {
			 System.out.println("Line is not supported"); }
			 
			 final TargetDataLine
			 targetDataLine=(TargetDataLine)AudioSystem.getLine(info);
			 targetDataLine.open();
			 System.out.println("starting Recording while 15sec.");
			 targetDataLine.start(); 
			 Thread stopper=new Thread(new Runnable() {
				 @Override public void run() { 
					 AudioInputStream audioStream=new AudioInputStream(targetDataLine); 
					 File wavFile=new File("sample.wav"); 
					 try {
						AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
					} catch (IOException e) {
						e.printStackTrace();
					} 
				} 
		 	}); 
			 stopper.start();
			 Thread.sleep(15000); 
			 targetDataLine.stop(); 
			 targetDataLine.close();
		}catch(Exception e) { 
			e.printStackTrace(); 
		}
	 }
	 
	 //녹음한 파일 파일로 작성
	 public static void file_writer(String result_text) {
	        
	        String message = result_text;
	        
	        File file = new File("sample.txt");
	        FileWriter writer = null;
	        BufferedWriter bWriter = null;

	        try {
	            writer = new FileWriter(file, true);
	            bWriter = new BufferedWriter(writer);
	            
	            bWriter.write(message);
	            bWriter.flush();
	            
	            System.out.println("DONE");


	        } catch(IOException e) {
	            e.printStackTrace();
	        } finally {
	        	 try {
	                 if(bWriter != null) bWriter.close();
	                 if(writer != null) writer.close();
	             } catch(IOException e) {
	                 e.printStackTrace();
	             }


	        }
	    }


	 
	 
	
	public static void main(String[] args) {
		//voiceRecord(); 직접 녹음을 할 경우 활성화
		final SttTest SttTest = new SttTest();
		String result = SttTest.upload(new File("sample.wav"), "sync", null, null, null, null);
		JSONObject J_result = new JSONObject(result);
		String result_text=(String) J_result.get("text");
		file_writer(result_text);
		System.out.println(result_text);
	}


}
