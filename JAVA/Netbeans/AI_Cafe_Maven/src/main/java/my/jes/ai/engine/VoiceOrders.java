/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package my.jes.ai.engine;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;
import com.mulcam.ai.web.vo.ProductVO;
import java.util.ArrayList;


import org.json.JSONArray;
import org.json.JSONObject;



/**
 *
 * @author javanism
 */
public class VoiceOrders {
    public static ArrayList<ProductVO> productList=new ArrayList<ProductVO>();
    public static boolean flag;

     public static String process(String voiceMessage) {

        String chatbotMessage = "";

        try {
            String apiURL = "https://854974ea07194c61ba70fdb1b3859611.apigw.ntruss.com/custom/v1/4325/1cbcb3f23d28a923b593568e6d5e79dcc52babe8684755ed53b13360db148a00";

            URL url = new URL(apiURL);

           // String voiceMessage="문 열어요?";
            String message = getReqMessage(voiceMessage);
            //System.out.println("##" + message);

            String secretKey="SVJUQnJxY3FFUUNGbVB2cFFnWVhkcEhxUWd0WnZNY0w=";
            String encodeBase64String = makeSignature(message, secretKey);

            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json;UTF-8");
            con.setRequestProperty("X-NCP-CHATBOT_SIGNATURE", encodeBase64String);

            // post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(message.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();

            BufferedReader br;

            if(responseCode==200) { // Normal call
                //System.out.println(con.getResponseMessage());

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                con.getInputStream(),"UTF-8"));
                String decodedString;
                while ((decodedString = in.readLine()) != null) {
                    chatbotMessage = decodedString;
                }
                //chatbotMessage = decodedString;
                in.close();

            } else {  // Error occurred
                chatbotMessage = con.getResponseMessage();
            }         

            System.out.println(chatbotMessage);
            /*
            {"version":"v2",
            "userId":"U47b00b58c90f8e47428af8b7bddc1231heo2jes",
            "timestamp":1617767683948,
            "bubbles":[{
                        "type":"text",
                        "data":{"description":"아이스 아메리카노 그란데 한잔을(를) 주문하시겠습니까?"},
                        "slot":[{"name":"커피종류","value":"아메리카노"},
                                {"name":"커피온도","value":"아이스"},
                                {"name":"커피사이즈","value":"그란데"},
                                {"name":"커피수량","value":"한잔"}
                                ],
                        "context":[]
                        }],
            "scenario":{"name":"커피주문","intent":["주문"]},
            "entities":[{"word":"한잔","name":"커피수량"}],
            "keywords":[],
            "event":"send"}
            */
            JSONObject o=new JSONObject(chatbotMessage);
            JSONArray bubbles=o.getJSONArray("bubbles");
            JSONObject bubbles0=bubbles.getJSONObject(0);
            JSONObject data=bubbles0.getJSONObject("data");
            String description=(String) data.get("description");
            System.out.println("--->"+description);
            
            if(description.contains("주문하시겠습니까")){
                JSONArray slot=bubbles0.getJSONArray("slot");
                ProductVO p=new ProductVO();
                slot.forEach((item)->{
                    System.out.print("item:"+item+"\t"+p+"\n");
                    JSONObject jo=(JSONObject)item;
                    String name=jo.getString("name");
                    if(name.contains("커피종류")){
                        p.setProduct_name(jo.getString("value"));
                    }else if(name.contains("커피온도")){
                        p.setProduct_name(jo.getString("value")+" "+p.getProduct_name());
                    }else if(name.contains("커피수량")){
                        String quantity_str=jo.getString("value");
                        int quantity=0;
                        if(quantity_str.contains("한")){
                            quantity=1;
                        }else if(quantity_str.contains("두")){
                            quantity=2;
                        }else if(quantity_str.contains("세")){
                            quantity=3;
                        }else if(quantity_str.contains("네")){
                            quantity=4;
                        }else if(quantity_str.contains("다섯")){
                            quantity=5;
                        }else{
                            System.out.println("커피수량 입력 오류");
                        }
                        p.setQuantity(quantity);                        
                    }
                });//end foreach
                productList.add(p);
            }
            return description;
        } catch (Exception e) {
            System.out.println(e);
            return "죄송합니다. 다시 말씀해주세요";
        }       
        
    }

    public static String makeSignature(String message, String secretKey) {

        String encodeBase64String = "";

        try {
            byte[] secrete_key_bytes = secretKey.getBytes("UTF-8");

            SecretKeySpec signingKey = new SecretKeySpec(secrete_key_bytes, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
            encodeBase64String = Base64.encodeToString(rawHmac, Base64.NO_WRAP);

            return encodeBase64String;

        } catch (Exception e){
            System.out.println(e);
        }

        return encodeBase64String;

    }

    public static String getReqMessage(String voiceMessage) {

        String requestBody = "";

        try {

            JSONObject obj = new JSONObject();

            long timestamp = new Date().getTime();

            //System.out.println("##"+timestamp);

            obj.put("version", "v2");
            obj.put("userId", "U47b00b58c90f8e47428af8b7bddc1231heo2jes");
//=> userId is a unique code for each chat user, not a fixed value, recommend use UUID. use different id for each user could help you to split chat history for users.

            obj.put("timestamp", timestamp);

            JSONObject bubbles_obj = new JSONObject();

            bubbles_obj.put("type", "text");

            JSONObject data_obj = new JSONObject();
            data_obj.put("description", voiceMessage);

            bubbles_obj.put("type", "text");
            bubbles_obj.put("data", data_obj);

            JSONArray bubbles_array = new JSONArray();
            bubbles_array.put(bubbles_obj);

            obj.put("bubbles", bubbles_array);
            obj.put("event", "send");

            requestBody = obj.toString();

        } catch (Exception e){
            System.out.println("## Exception : " + e);
        }

        return requestBody;

    }
    

}
