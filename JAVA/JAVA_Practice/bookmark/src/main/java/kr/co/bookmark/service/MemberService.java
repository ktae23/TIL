package kr.co.bookmark.service;

import java.util.List;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.MemberVO;

@Repository 
public interface MemberService {
	public void memberInsert(MemberVO m) throws Exception;
	public String login(MemberVO m) throws Exception;
	public List<MemberVO> memberList() throws Exception;

}
