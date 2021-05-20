package kr.co.openeg.mapper;

import java.util.List;

import kr.co.openeg.domain.BoardAttachVO;

public interface BoardAttachMapper {
	public void insert(BoardAttachVO vo);

	public void delete(String uuid);

	public List<BoardAttachVO> findByBno(Long bno);

	public void deleteAllByBno(Long bno);
}
