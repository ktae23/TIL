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
		//유효성 검사
		if(prodCode!=null) {
			this.prodCode = prodCode;
		}else {
			System.out.println("상품코드는 널이 될 수 없습니다.");
		}
	}
	public String getProdName() {
		return prodName;
	}
	public void setProdName(String prodName) {
		if(prodName!=null) {
			this.prodName = prodName;
		}else {
			System.out.println("상품명은 널이 될 수 없습니다.");
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