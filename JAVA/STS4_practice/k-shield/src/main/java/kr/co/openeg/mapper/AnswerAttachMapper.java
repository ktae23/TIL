package kr.co.openeg.mapper;

import java.util.List;

import kr.co.openeg.domain.AnswerAttachVO;

public interface AnswerAttachMapper {
	public void insert(AnswerAttachVO asvo);

	public void delete(String uuid);

	public List<AnswerAttachVO> findByAsno(Long asno);

	public void deleteAllByAsno(Long asno);
}
