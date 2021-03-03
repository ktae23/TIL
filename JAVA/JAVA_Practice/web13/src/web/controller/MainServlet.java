package web.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import web.model.MemberDAO;
import web.util.MemberVO;
import web.util.MyException;

@WebServlet("/main")
public class MainServlet extends HttpServlet {
	MemberDAO mDao;
	
	@Override
	public void init() throws ServletException {
		try {
			mDao=new MemberDAO();
		} catch (MyException e) {
			System.out.println(e.getMessage());
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String sign = request.getParameter("sign");
		if(sign==null) {
			return;
		}else if(sign.equals("login")) {
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			PrintWriter out=response.getWriter();
			out.write(id+":"+pw);
			}
		else if(sign.equals("memberInsert")) {
			response.setContentType("text/html;charset-utf-8");
			PrintWriter out=response.getWriter();
			
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			String name=request.getParameter("name");
			String [] all_subject=request.getParameterValues("subject");

			for(String s:all_subject) {
//				out.write(s+"&nbsp; ");
			}
			MemberVO m=new MemberVO(id,name);
			try {
				mDao.memberInsert(m);
				out.write("회원가입 완료되었습니다.");
			} catch (MyException e) {
				out.write(e.getMessage());
			}
			
		}
	}
}

