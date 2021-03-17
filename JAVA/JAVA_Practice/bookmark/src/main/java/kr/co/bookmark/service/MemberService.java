package kr.co.bookmark.service;

import java.util.List;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.MemberVO;

@Repository 
public interface MemberService {
	public void memberInsert(MemberVO m) throws Exception;
	public String memberlogin(MemberVO m) throws Exception;
	public List<MemberVO> memberList() throws Exception;
	public void memberDelete(MemberVO m)throws Exception;
	public void memberUpdate(MemberVO m)throws Exception;

}
