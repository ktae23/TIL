package com.mulcam.ai.web.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mulcam.ai.web.dao.OrderDAO;
import com.mulcam.ai.web.vo.OrderVO;

@Service
public class OrderService {

	@Autowired
	OrderDAO orderDAO;

	public long insert(ArrayList<OrderVO> list) {
		return orderDAO.insert(list);
	}

}
