package generic_practice;

public class ProductTest {

	public static void main(String[] args) {
		Product<String, Integer> product1 = new Product<String, Integer>();
		product1.setKind("TV");
		product1.setModel(151105);
		
		System.out.println(product1);
		
		String kind1 = product1.getKind();
		int model1 = product1.getModel();
				
		System.out.println(kind1 + "," + model1);
	}
}
