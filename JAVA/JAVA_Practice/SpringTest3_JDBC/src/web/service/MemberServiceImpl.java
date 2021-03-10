package web.service;

import java.util.List;

import web.dao.MemberDAO;

public class MemberServiceImpl implements MemberService{
	
	private MemberDAO memberDAO;
	
	

	public void setMemberDAO(MemberDAO memberDAO) {
		this.memberDAO = memberDAO;
	}



	@Override
	public List listMembers() {
		List membersList=memberDAO.selectAllMembers();
		return membersList;
	}

}