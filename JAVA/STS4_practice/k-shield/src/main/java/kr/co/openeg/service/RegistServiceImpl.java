package kr.co.openeg.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.openeg.common.CommonCode;
import kr.co.openeg.domain.Criteria;
import kr.co.openeg.domain.RegistAttachVO;
import kr.co.openeg.domain.RegistVO;
import kr.co.openeg.mapper.RegistAttachMapper;
import kr.co.openeg.mapper.RegistMapper;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Service
@AllArgsConstructor
public class RegistServiceImpl implements RegistService {

	@Setter(onMethod_ = @Autowired)
	private RegistMapper registMapper;
	
	@Setter(onMethod_ = @Autowired)
	private RegistAttachMapper attachMapper;

	@Transactional
	@Override
	public void register(RegistVO rgvo) {
		registMapper.insertSelectKey(rgvo);
		
		if (rgvo.getAttachList() == null || rgvo.getAttachList().size() <= 0) {
			return;
		}
		
		rgvo.getAttachList().forEach(attach -> {
			attach.setRgno(rgvo.getRgno());
			attachMapper.insert(attach);
		});
	}

	@Override
	public RegistVO get(Long rgno) {
		return registMapper.read(rgno);
	}

	@Transactional
	@Override
	public boolean modify(RegistVO rgvo) {
		
		boolean modifyResult = registMapper.update(rgvo) == 1;

		if (modifyResult && rgvo.getAttachList().size() > 0) {
			attachMapper.deleteAllByRgno(rgvo.getRgno());

			rgvo.getAttachList().forEach(attach -> {
				attach.setRgno(rgvo.getRgno());
				attachMapper.insert(attach);
			});
		}

		return modifyResult;
	}

	@Transactional
	@Override
	public boolean remove(RegistVO rgvo) {
		attachMapper.deleteAllByRgno(rgvo.getRgno());
		return registMapper.delete(rgvo) == 1;
	}

	@Override
	public List<RegistVO> getList(Criteria cri) {
		return registMapper.getListWithPaging(cri);
	}

	@Override
	public int getTotal(Criteria cri) {
		return registMapper.getTotalCount(cri);
	}
	
	@Override
	public List<RegistAttachVO> getAttachList(Long rgno) {
		return attachMapper.findByRgno(rgno);
	}

	@Override
	public void removeAttach(Long rgno) {
		attachMapper.deleteAllByRgno(rgno);
	}
	
	@Override
	public RegistVO checkEmail(String email) {
		return registMapper.checkEmail(email);
	}
	
	@Override
	public RegistVO checkUser(RegistVO rgvo) {
		rgvo.setUserRole(CommonCode.CONT_USER_ROLE_MEMBER);
		return registMapper.checkUser(rgvo);
	}
	
	@Override
	public RegistVO checkAdmin(RegistVO rgvo) {
		rgvo.setUserRole(CommonCode.CONT_USER_ROLE_ADMIN);
		return registMapper.checkUser(rgvo);
	}
	
	@Override
	public boolean insertSalt(RegistVO rgvo) {
		return registMapper.insertSalt(rgvo) > 0;
	}
	
	@Override
	public String selectSalt(String email) {
		return StringUtils.defaultString(registMapper.selectSalt(email));
	}

}
