package server.service;

import java.util.ArrayList;

import common.entity.Member;
import common.util.CafeException;
import server.dao.MemberDAO;

public class MemberService {
	
	MemberDAO mdao;
	
	public MemberService() throws CafeException {
		mdao=new MemberDAO();
	}

	public void insertMember(Member m) throws CafeException {
		mdao.insertMember(m);
		
	}

	public ArrayList<Member> selectMember() throws CafeException {
		return mdao.selectMember();		
	}

}