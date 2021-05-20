package kr.co.openeg.mapper;

import java.util.List;

import kr.co.openeg.domain.RegistAttachVO;

public interface RegistAttachMapper {
	public void insert(RegistAttachVO rgvo);

	public void delete(String uuid);

	public List<RegistAttachVO> findByRgno(Long rgno);

	public void deleteAllByRgno(Long rgno);
}
