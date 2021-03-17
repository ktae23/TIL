package kr.co.bookmark.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.co.bookmark.dao.MemberDAO;
import kr.co.bookmark.vo.MemberVO;

@Service
public class MemberServiceImpl implements MemberService{
	@Autowired
	MemberDAO memberDAO;
	
	public void memberInsert(MemberVO m) throws Exception{
		memberDAO.memberInsert(m);
	}
	
	public String memberlogin(MemberVO m) {
		System.out.println("서비스 멤버 로그인 호출");
		return memberDAO.login(m);		
	}

	@Override
	public List<MemberVO> memberList() throws Exception {
		System.out.println("서비스 멤버 리스트 호출");
		return memberDAO.memberList();
	}

	@Override
	public void memberDelete(MemberVO m) throws Exception {
		System.out.println("서비스 멤버 삭제 호출");
		memberDAO.memberDelete(m);
		
	}

	@Override
	public void memberUpdate(MemberVO m) throws Exception {
		System.out.println("서비스 멤버 수정 호출");
		memberDAO.memberUpdate(m);
		
	}
	
}
