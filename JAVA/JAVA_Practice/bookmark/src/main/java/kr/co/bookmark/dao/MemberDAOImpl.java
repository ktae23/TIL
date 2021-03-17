package kr.co.bookmark.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.MemberVO;

@Repository
public class MemberDAOImpl implements MemberDAO{
	
	@Autowired
	SqlSession sqlSession;
	
	public void memberInsert(MemberVO m) {
		System.out.println("멤버 인서트 호출");
		sqlSession.insert("mapper.member.memberInsert",m);
	}
	

	public String login(MemberVO m) {
		System.out.println("멤버 DAO 로그인 호출");
		return sqlSession.selectOne("mapper.member.login",m);		
	}	
	
	public List<MemberVO> memberList() {
		System.out.println("멤버 DAO 리스트 호출");
		return sqlSession.selectList("selectAllMemberList");
		
	}


	@Override
	public void memberDelete(MemberVO m) {
		System.out.println("멤버 딜리트 호출");
		sqlSession.insert("mapper.member.deleteMember",m);
	}


	@Override
	public void memberUpdate(MemberVO m) {
		System.out.println("멤버 업데이트 호출");
		sqlSession.insert("mapper.member.updateMember",m);
	}

}
