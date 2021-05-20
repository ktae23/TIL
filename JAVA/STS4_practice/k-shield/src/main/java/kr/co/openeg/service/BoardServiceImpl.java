package kr.co.openeg.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.openeg.domain.BoardAttachVO;
import kr.co.openeg.domain.BoardVO;
import kr.co.openeg.domain.Criteria;
import kr.co.openeg.mapper.BoardAttachMapper;
import kr.co.openeg.mapper.BoardMapper;
import kr.co.openeg.mapper.ReplyMapper;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Service
@AllArgsConstructor
public class BoardServiceImpl implements BoardService {

	@Setter(onMethod_ = @Autowired)
	private BoardMapper boardMapper;
	
	@Setter(onMethod_ = @Autowired)
	private ReplyMapper replyMapper;
	
	@Setter(onMethod_ = @Autowired)
	private BoardAttachMapper attachMapper;

	@Transactional
	@Override
	public void register(BoardVO board) {
		boardMapper.insertSelectKey(board);
		
		if (board.getAttachList() == null || board.getAttachList().size() <= 0) {
			return;
		}
		
		board.getAttachList().forEach(attach -> {
			attach.setBno(board.getBno());
			attachMapper.insert(attach);
		});
	}

	@Override
	public BoardVO get(Long bno) {
		return boardMapper.read(bno);
	}

	@Transactional
	@Override
	public boolean modify(BoardVO board) {
		boolean modifyResult = boardMapper.update(board) == 1;
		if (modifyResult && board.getAttachList().size() > 0) {
			attachMapper.deleteAllByBno(board.getBno());
			
			board.getAttachList().forEach(attach -> {
				attach.setBno(board.getBno());
				attachMapper.insert(attach);
			});
		}

		return modifyResult;
	}

	@Transactional
	@Override
	public boolean remove(Long bno) {
		replyMapper.deleteAllByBno(bno);
		attachMapper.deleteAllByBno(bno);
		return boardMapper.delete(bno) == 1;
	}

	@Override
	public List<BoardVO> getList(Criteria cri) {
		return boardMapper.getListWithPaging(cri);
	}

	@Override
	public int getTotal(Criteria cri) {
		return boardMapper.getTotalCount(cri);
	}
	
	@Override
	public List<BoardAttachVO> getAttachList(Long bno) {
		return attachMapper.findByBno(bno);
	}

	@Override
	public void removeAttach(Long bno) {
		attachMapper.deleteAllByBno(bno);
	}

}
