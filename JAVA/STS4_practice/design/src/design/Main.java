package design;

import design.strategy.AppendStrategy;
import design.strategy.Base64Strategy;
import design.strategy.Encoder;
import design.strategy.EncodingStrategy;
import design.strategy.NormalStrategy;

public class Main {

	public static void main(String[] args) {
		
		Encoder encoder = new Encoder();
		
		// base64
		EncodingStrategy base64 = new Base64Strategy();
		
		// noramal
		EncodingStrategy normal = new NormalStrategy();
		
		String message = "hello java";
		
		encoder.setEncodingStrategy(base64);
		String base64Result = encoder.getMessage(message);
		System.out.println(base64Result);
		
		encoder.setEncodingStrategy(normal);
		String normalResult = encoder.getMessage(message);
		System.out.println(normalResult);
		
		encoder.setEncodingStrategy(new AppendStrategy());
		String appendResult = encoder.getMessage(message);
		System.out.println(appendResult);	
		
	}
}
	
