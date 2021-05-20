package kr.co.openeg.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.openeg.domain.AnswerAttachVO;
import kr.co.openeg.domain.AnswerVO;
import kr.co.openeg.mapper.AnswerAttachMapper;
import kr.co.openeg.mapper.AnswerMapper;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Service
@AllArgsConstructor
public class AnswerServiceImpl implements AnswerService {

	@Setter(onMethod_ = @Autowired)
	private AnswerMapper answerMapper;
	
	@Setter(onMethod_ = @Autowired)
	private AnswerAttachMapper attachMapper;

	@Transactional
	@Override
	public void answer(AnswerVO asvo) {
		answerMapper.insertSelectKey(asvo);
		
		if (asvo.getAttachList() == null || asvo.getAttachList().size() <= 0) {
			return;
		}
		
		asvo.getAttachList().forEach(attach -> {
			attach.setAsno(asvo.getAsno());
			attachMapper.insert(attach);
		});
	}

	@Override
	public AnswerVO get(AnswerVO asvo) {
		return answerMapper.read(asvo);
	}

	@Override
	public Long getAsno(Long rgno) {
		return answerMapper.getAsno(rgno);
	}
	
	@Transactional
	@Override
	public boolean modify(AnswerVO asvo) {
		
		boolean modifyResult = answerMapper.update(asvo) == 1;

		if (modifyResult && asvo.getAttachList().size() > 0) {
			attachMapper.deleteAllByAsno(asvo.getAsno());

			asvo.getAttachList().forEach(attach -> {
				attach.setAsno(asvo.getAsno());
				attachMapper.insert(attach);
			});
		}

		return modifyResult;
	}

	@Transactional
	@Override
	public boolean remove(AnswerVO asvo) {
		attachMapper.deleteAllByAsno(asvo.getAsno());
		return answerMapper.delete(asvo) == 1;
	}
	
	@Override
	public List<AnswerAttachVO> getAttachList(Long asno, Long rgno) {
		return attachMapper.findByAsno(asno);
	}

	@Override
	public void removeAttach(Long asno, Long rgno) {
		attachMapper.deleteAllByAsno(asno);
	}
	
}
