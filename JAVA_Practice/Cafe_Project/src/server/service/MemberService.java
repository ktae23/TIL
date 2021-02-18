package server.service;

import java.sql.SQLException;

import common.entity.Member;
import common.util.CafeException;
import server.dao.MemberDAO;

public class MemberService {

	MemberDAO mdao; 
	
	public MemberService () throws CafeException {
		mdao=new MemberDAO(); //MemberDAO와 수명을 같이 한다
	}

	public void insertMember(Member m) throws CafeException{
		mdao.insertMember(m);
	}
}
