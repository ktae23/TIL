package kr.co.openeg.service;

import java.util.List;

import kr.co.openeg.domain.Criteria;
import kr.co.openeg.domain.ReplyPageDTO;
import kr.co.openeg.domain.ReplyVO;

public interface ReplyService {

	public int register(ReplyVO vo);

	public ReplyVO get(Long rno);

	public int modify(ReplyVO vo);

	public int remove(Long rno);
	
	public int removeAllByBno(Long bno);

	public List<ReplyVO> getList(Criteria cri, Long bno);
	
	public ReplyPageDTO getListPage(Criteria cri, Long bno);

}
