package example3;

public class OracleDao implements DataAccessObject {

	@Override
	public String select() {
		System.out.println("Oracle DB에서 검색");
		return null;
	}

	@Override
	public String insert() {
		System.out.println("Oracle DB에서 삽입");
		return null;
	}

	@Override
	public String update() {
		System.out.println("Oracle DB에서 수정");
		return null;
	}

	@Override
	public String delete() {
		System.out.println("Oracle DB에서 삭제");
		return null;
	}
	

}
