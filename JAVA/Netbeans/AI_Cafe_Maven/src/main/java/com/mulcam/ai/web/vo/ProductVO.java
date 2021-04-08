package com.mulcam.ai.web.vo;

public class ProductVO{
    private String product_name;
    private int quantity;

    public void setProduct_name(String product_name){
        this.product_name=product_name;
    }

    public String getProduct_name(){
        return product_name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ProductVO{" + "product_name=" + product_name + ", quantity=" + quantity + '}';
    }
    
    

}
