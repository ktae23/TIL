package com.mulcam.ai.web.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mulcam.ai.web.vo.OrderVO;

@Repository
public class OrderDAOImpl {

	@Autowired
	SqlSession sqlSession;
	
	public long insert(ArrayList<OrderVO> list) {
		//System.out.println(list.size());
		long order_group_no=getOrder_group_no();
		for(OrderVO orderVO:list) {		
			orderVO.setOrder_group_no(order_group_no);
			//System.out.println(">>>"+orderVO);
			sqlSession.insert("mapper.order.insert",orderVO);
		}
		return order_group_no;
	}
	
	private long getOrder_group_no() {
		long order_group_no=sqlSession.selectOne("mapper.order.order_group_no");
		//System.out.println(order_group_no);
		return order_group_no;
	}

	public List<OrderVO> ordersSelect() {
		return sqlSession.selectList("mapper.order.select");
	}

	public void update(long order_group_no) {
		sqlSession.update("mapper.order.update",order_group_no);
		
	}


}
