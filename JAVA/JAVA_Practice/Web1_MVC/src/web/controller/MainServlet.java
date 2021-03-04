package web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import web.model.MemberDAO;
import web.util.Member;
import web.util.MyException;


@WebServlet("/Main")
public class MainServlet extends HttpServlet {
	
	MemberDAO mDao;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			mDao=new MemberDAO();
		} catch (MyException e) {
			System.out.println(e.getMessage());
		}
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			request.setCharacterEncoding("utf-8");
			String key = request.getParameter("key");
			
			if(key==null) {
				throw new MyException("요청을 구별 할 수 없습니다.");
			}else if(key.equalsIgnoreCase("login")) {
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");
				String name=mDao.login(id,pw);
				if(name!=null) {
					RequestDispatcher disp=request.getRequestDispatcher("login_ok.jsp");
					request.setAttribute("name",name);
					disp.forward(request, response);
				}else {
					RequestDispatcher disp=request.getRequestDispatcher("login_fail.jsp");
					disp.forward(request, response);
				}
			}else if(key.equalsIgnoreCase("memberList")) {
				List<Member> list=mDao.memberList();
				RequestDispatcher disp=request.getRequestDispatcher("memberList_ok.jsp");
				request.setAttribute("list", list);
				disp.forward(request, response);
			}else if(key.equalsIgnoreCase("memberInsert")) {
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");
				String name=request.getParameter("name");
				Member m = new Member(id,pw,name);
				mDao.memberInsert(m);
				
				RequestDispatcher disp=request.getRequestDispatcher("memberInsert_ok.jsp");
				disp.forward(request, response);

			}else if(key.equalsIgnoreCase("memberDelete")) {
				String id=request.getParameter("id");			
				mDao.deleteMember(id);
				
				RequestDispatcher disp=request.getRequestDispatcher("memberDelete_ok.jsp");	
				request.setAttribute("id", id);
				disp.forward(request, response);
			}
			
		}catch(MyException e) {
			RequestDispatcher disp=request.getRequestDispatcher("error.jsp");
			disp.forward(request, response);
		}
	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}

}
