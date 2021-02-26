package server.service;

import common.entity.OrderDTO;
import common.util.CafeException;
import server.dao.OrderDAO;

public class OrderService {
	
	OrderDAO odao;
	
	public OrderService() throws CafeException {
		odao=new OrderDAO();
	}
	
	public int insertOrder(OrderDTO o) throws CafeException {
		return odao.insertOrder(o);
	}

}