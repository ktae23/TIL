package com.mulcam.ai.web.vo;

import java.util.Date;

public class OrderVO {
	
	private String ordermethod,product_name;
	private Long oreder_group_no, quantity;
	private Date orderdate;
	
	
	
	public OrderVO(String ordermethod, String product_name, Long quantity) {
		super();
		this.ordermethod = ordermethod;
		this.product_name = product_name;
		this.quantity = quantity;
	}
	public OrderVO() {
		super();
		// TODO Auto-generated constructor stub
	}
	public String getOrdermethod() {
		return ordermethod;
	}
	public void setOrdermethod(String ordermethod) {
		this.ordermethod = ordermethod;
	}
	public String getProduct_name() {
		return product_name;
	}
	public void setProduct_name(String product_name) {
		this.product_name = product_name;
	}
	public Long getOreder_group_no() {
		return oreder_group_no;
	}
	public void setOreder_group_no(Long oreder_group_no) {
		this.oreder_group_no = oreder_group_no;
	}
	public Long getQuantity() {
		return quantity;
	}
	public void setQuantity(Long quantity) {
		this.quantity = quantity;
	}
	public Date getOrderdate() {
		return orderdate;
	}
	public void setOrderdate(Date orderdate) {
		this.orderdate = orderdate;
	}
	@Override
	public String toString() {
		return "OrderVO [ordermethod=" + ordermethod + ", product_name=" + product_name + ", oreder_group_no="
				+ oreder_group_no + ", quantity=" + quantity + ", orderdate=" + orderdate + "]";
	}
	
	
	
	
}
