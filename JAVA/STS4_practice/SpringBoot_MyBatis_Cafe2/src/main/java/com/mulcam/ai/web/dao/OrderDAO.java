package com.mulcam.ai.web.dao;

import java.util.ArrayList;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.mulcam.ai.web.vo.OrderVO;

@Repository
@Mapper
public interface OrderDAO {

	public long insert(ArrayList<OrderVO> list);
}
