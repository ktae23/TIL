package kr.co.openeg.mapper;

import kr.co.openeg.domain.AnswerVO;

public interface AnswerMapper {

	public Integer insertSelectKey(AnswerVO asvo);

	public AnswerVO read(AnswerVO asvo);

	public int delete(AnswerVO asvo);

	public int update(AnswerVO asvo);

	public Long getAsno(Long rgno);
}
