package kr.co.openeg.service;

import java.util.List;

import kr.co.openeg.domain.BoardAttachVO;
import kr.co.openeg.domain.BoardVO;
import kr.co.openeg.domain.Criteria;

public interface BoardService {

	public void register(BoardVO board);

	public BoardVO get(Long bno);

	public boolean modify(BoardVO board);

	public boolean remove(Long bno);

	public List<BoardVO> getList(Criteria cri);

	public int getTotal(Criteria cri);

	public List<BoardAttachVO> getAttachList(Long bno);

	public void removeAttach(Long bno);
}
