package student;

import common.entity.StudentDTO;
import common.util.StudentException;
import server.dao.StudentDAO;

public class StudentTest {

	public static void main(String[] args) throws StudentException {

		StudentDAO sDAO = new StudentDAO();
//		StudentDTO sDTO = new StudentDTO(3,  "���浿",  "�����а�",  "����",  "010-3333-3333");
//		sDAO.insertStudent(sDTO);
		sDAO.printAllStudent();
		
		
 
		  
	}

}
