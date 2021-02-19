package common.entity;

import java.util.Date;

public class OrderDTO {
	
	private int orderNo,quantity;
	private String memberId, prodCode,orderMethod;
	private Date orderDate;	
	
	
	public OrderDTO() {
		super();
		// TODO Auto-generated constructor stub
	}	

	public OrderDTO(int quantity, String memberId, String prodCode, String orderMethod) {
		super();
		setQuantity(quantity);
		setMemberId(memberId);
		setProdCode(prodCode);
		setOrderMethod(orderMethod);
	}

	public OrderDTO(int orderNo, int quantity, String memberId, String prodCode, String orderMethod, Date orderDate) {
		this(quantity,memberId,prodCode,orderMethod);
		setOrderNo(orderNo);		
		setOrderDate(orderDate);
	}
	
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		if(orderNo>0) {
			this.orderNo = orderNo;
		}else {
			System.out.println("주문번호가 옳지 않음");
		}
	}
	public int getQuantity() {
		return quantity;
	}
	public void setQuantity(int quantity) {
		if(quantity>0) {
			this.quantity = quantity;
		}else {
			System.out.println("수량을 확인해 주세요");
		}
	}
	public String getMemberId() {
		return memberId;
	}
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	public String getProdCode() {
		return prodCode;
	}
	public void setProdCode(String prodCode) {
		this.prodCode = prodCode;
	}
	public String getOrderMethod() {
		return orderMethod;
	} 
	public void setOrderMethod(String orderMethod) {
		this.orderMethod = orderMethod;
	}
	public Date getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}
	
	

}