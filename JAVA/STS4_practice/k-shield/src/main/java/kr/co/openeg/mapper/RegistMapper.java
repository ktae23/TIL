package kr.co.openeg.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import kr.co.openeg.domain.RegistVO;
import kr.co.openeg.domain.Criteria;

public interface RegistMapper {

	public List<RegistVO> getList();

	public List<RegistVO> getListWithPaging(Criteria cri);

	// public void insert(RegistVO rgvo);

	public Integer insertSelectKey(RegistVO rgvo);

	public RegistVO read(Long rgno);

	public int delete(RegistVO rgvo);

	public int update(RegistVO rgvo);

	public int getTotalCount(Criteria cri);
	
	public RegistVO checkUser(RegistVO rgvo);

	public RegistVO checkEmail(String email);
	
	public int insertSalt(RegistVO rgvo);
	
	public String selectSalt(String email);
	
	
}
