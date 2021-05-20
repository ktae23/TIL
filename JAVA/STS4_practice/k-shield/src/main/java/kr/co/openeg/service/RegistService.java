package kr.co.openeg.service;

import java.util.List;

import kr.co.openeg.domain.Criteria;
import kr.co.openeg.domain.RegistAttachVO;
import kr.co.openeg.domain.RegistVO;

public interface RegistService {

	public void register(RegistVO rgvo);

	public RegistVO get(Long rgno);

	public boolean modify(RegistVO rgvo);

	public boolean remove(RegistVO rgvo);

	public List<RegistVO> getList(Criteria cri);

	public int getTotal(Criteria cri);

	public List<RegistAttachVO> getAttachList(Long rgno);

	public void removeAttach(Long rgno);
	
	public RegistVO checkEmail(String email);

	public RegistVO checkUser(RegistVO rgvo);

	public RegistVO checkAdmin(RegistVO rgvo);
	
	public boolean insertSalt(RegistVO rgvo);
	
	public String selectSalt(String email);
}
