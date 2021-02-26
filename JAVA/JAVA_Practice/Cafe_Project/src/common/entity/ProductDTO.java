package common.entity;

public class ProductDTO {
	
	private String prodCode;
	private String prodName;
	private int price;
		
	public ProductDTO() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public ProductDTO(String prodCode, String prodName, int price) {
		setProdCode(prodCode);
		setProdName(prodName);
		setPrice(price);
	}
	
	public String getProdCode() {
		return prodCode;
	}
	public void setProdCode(String prodCode) {
		//��ȿ�� �˻�
		if(prodCode!=null) {
			this.prodCode = prodCode;
		}else {
			System.out.println("��ǰ�ڵ�� ���� �� �� �����ϴ�.");
		}
	}
	public String getProdName() {
		return prodName;
	}
	public void setProdName(String prodName) {
		if(prodName!=null) {
			this.prodName = prodName;
		}else {
			System.out.println("��ǰ���� ���� �� �� �����ϴ�.");
		}
	}
	public int getPrice() {
		return price;
	}
	public void setPrice(int price) {
		if(price>0) {
			this.price = price;
		}
	}
	
	

}