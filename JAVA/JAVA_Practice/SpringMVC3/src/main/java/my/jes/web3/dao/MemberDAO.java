package my.jes.web3.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import my.jes.web3.vo.MemberVO;

@Repository
public class MemberDAO {

	@Autowired
	SqlSession sqlSession;
	
	public String login(String id,String pw) {
		return sqlSession.selectOne("mapper.member.login", new MemberVO(id,pw));
	}
}
