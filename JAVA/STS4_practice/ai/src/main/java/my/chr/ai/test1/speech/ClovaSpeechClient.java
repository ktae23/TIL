package my.chr.ai.test1.speech;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.google.gson.Gson;

public class ClovaSpeechClient {

	private static final String SECRET = "ffaed44214be4fabacc59a7fbedc6020";
	private static final String INVOKE_URL = "https://clovaspeech-gw.ncloud.com/external/v1/273/4a8ecfcbec2aa4e7166cff4898f634b1f9dcd2828ef2061096837b92c3e222f9";

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

	/**
	 * recognize media using URL
	 * @param url required, the media URL
	 * @param completion optional, sync/async
	 * @param callback optional, used to receive the analyzed results
	 * @param userdata optional, any data
	 * @param forbiddens optional, comma separated words
	 * @param boostings optional, object array => [{"words":"comma separated words"}]
	 * @return
	 */
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

	/**
	 * recognize media using Object Storage
	 * @param dataKey required, the Object Storage key
	 * @param completion optional, sync/async
	 * @param callback optional, used to receive the analyzed results
	 * @param userdata optional, , any data
	 * @param forbiddens optional, comma separated words
	 * @param boostings  optional, object array => [{"words":"comma separated words"}]
	 * @return
	 */
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

	/**
	 *
	 * recognize media using a file
	 * @param file required, the media file
	 * @param completion optional, sync/async
	 * @param callback optional, used to receive the analyzed results
	 * @param userdata optional, any data
	 * @param forbiddens optional, comma separated words
	 * @param boostings object array => [{"words":"comma separated words"}]
	 * @return
	 */
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

	public static void main(String[] args) {
		final ClovaSpeechClient clovaSpeechClient = new ClovaSpeechClient();
		final String result = clovaSpeechClient.upload(new File("sample_long.wav"), "sync", null, null, null, null);
		//final String result = clovaSpeechClient.url("file URL", "sync", null, null, "", null);
		//final String result = clovaSpeechClient.objectStorage("Object Storage key", "sync", null, null, "", null);
		System.out.println(result);
	}
}
