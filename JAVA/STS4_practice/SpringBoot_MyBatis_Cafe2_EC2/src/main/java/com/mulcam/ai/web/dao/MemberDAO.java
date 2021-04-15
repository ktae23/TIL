package com.mulcam.ai.web.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.mulcam.ai.web.vo.MemberVO;

@Mapper
@Repository
public interface MemberDAO {
	public void memberInsert(MemberVO memberVO);

	public String login(MemberVO memberVO);
}
