package server.service;

import java.util.ArrayList;

import common.entity.MemberDTO;
import common.util.CafeException;
import server.dao.MemberDAO;

public class MemberService {
	
	MemberDAO mdao;
	
	public MemberService() throws CafeException {
		mdao=new MemberDAO();
	}

	public void insertMember(MemberDTO m) throws CafeException {
		mdao.insertMember(m);
		
	}

	public ArrayList<MemberDTO> selectMember() throws CafeException {
		return mdao.selectMember();		
	}

	public String selectMember(String memId) throws CafeException {
		return mdao.selectMember(memId);
		
	}

}