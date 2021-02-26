package server.service;

import java.util.ArrayList;

import common.entity.ProductDTO;
import common.util.CafeException;
import server.dao.ProductDAO;

public class ProductService {
	
	ProductDAO pdao;
	
	public ProductService() throws CafeException {
		pdao=new ProductDAO();
	}

	public void insertProduct(ProductDTO p) throws CafeException {
		pdao.insertProduct(p);
		
	}

	public ArrayList<ProductDTO> selectProduct() throws CafeException {
		return pdao.selectProduct();		
	}

	public String selectProduct(String prodCode) throws CafeException {
		return pdao.selectProduct(prodCode);
		
	}

}