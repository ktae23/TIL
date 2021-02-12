package wildcard;

public class Test {

	public static void main(String[] args) {
		
		ChildProduct<Tv, String, String> product = new ChildProduct<>();
		product.setKind(new Tv());
		product.setModel("Smart TV");
		product.setCompany("Samsung");
		
		System.out.println(product);
		System.out.println("제조사 : " + product.getCompany());
		System.out.println("품종과 제품 모델 : " + product.getKind().getTv() +", " + product.getModel());
		
		Storage<Tv> storage = new StorageImpl<>(100);
		storage.add(new Tv(), 0);
		Tv tv = storage.get(0);
		
		System.out.println(tv);
	}
}
