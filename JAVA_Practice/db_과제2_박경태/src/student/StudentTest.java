package student;

import common.entity.StudentDTO;
import common.util.StudentException;
import server.dao.StudentDAO;

public class StudentTest {

	public static void main(String[] args) throws StudentException {

		StudentDAO sDAO = new StudentDAO();
//		StudentDTO sDTO = new StudentDTO(3,  "나길동",  "영문학과",  "제주",  "010-3333-3333");
//		sDAO.insertStudent(sDTO);
		sDAO.printAllStudent();
		
		
 
		  
	}

}
