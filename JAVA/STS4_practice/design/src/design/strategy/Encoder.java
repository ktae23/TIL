package design.strategy;

public class Encoder {
	
	private EncodingStrategy encodingStrategy;
	
	public String getMessage (String messge){
		return this.encodingStrategy.encode(messge);
		
	}
	
	public void setEncodingStrategy(EncodingStrategy enCodingStrategy) {
		this.encodingStrategy = enCodingStrategy;
	}

}
