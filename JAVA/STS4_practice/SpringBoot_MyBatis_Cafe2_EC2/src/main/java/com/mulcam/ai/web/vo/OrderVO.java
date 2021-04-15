package com.mulcam.ai.web.vo;

import java.io.Serializable;
import java.util.Date;

public class OrderVO implements Serializable{
	private String ordermethod, product_name;
	private Long order_group_no, quantity;
	private Date orderdate;
	
	public OrderVO(String ordermethod, String product_name, Long quantity) {
		super();
		this.ordermethod = ordermethod;
		this.product_name = product_name;
		this.quantity = quantity;
	}

	public OrderVO() {
		super();
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
	
	public Long getOrder_group_no() {
		return order_group_no;
	}
	
	public void setOrder_group_no(Long order_group_no) {
		this.order_group_no = order_group_no;
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
		return "OrderVO [ordermethod=" + ordermethod + ", product_name=" + product_name + ", order_group_no="
				+ order_group_no + ", quantity=" + quantity + ", orderdate=" + orderdate + "]";
	}

}
