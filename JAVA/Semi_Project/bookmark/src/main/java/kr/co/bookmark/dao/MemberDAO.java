package kr.co.bookmark.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import kr.co.bookmark.vo.MemberVO;

	@Repository
	public interface MemberDAO {
		public void memberInsert(MemberVO m);
		public String login(MemberVO m);
		public List<MemberVO> memberList();
		public void memberDelete(MemberVO m);
		public void memberUpdate(MemberVO m);
		public int idCheck(MemberVO m);
		}

