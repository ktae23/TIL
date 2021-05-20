package kr.co.openeg.service;

import java.util.List;

import kr.co.openeg.domain.AnswerAttachVO;
import kr.co.openeg.domain.AnswerVO;

public interface AnswerService {

	public void answer(AnswerVO asvo);
	
	public AnswerVO get(AnswerVO asvo);

	public Long getAsno(Long rgno);
	
	public boolean modify(AnswerVO asvo);

	public boolean remove(AnswerVO asvo);

	public List<AnswerAttachVO> getAttachList(Long asno, Long rgno);

	public void removeAttach(Long asno, Long rgno);
	
}
