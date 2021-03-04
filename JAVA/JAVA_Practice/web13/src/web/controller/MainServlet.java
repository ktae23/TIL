package web.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
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

@WebServlet("/main")
public class MainServlet extends HttpServlet {
	
	MemberDAO mDao;
	
	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
		
		 try {
			mDao=new MemberDAO();
		} catch (MyException e) {
			System.out.println(e.getMessage());
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		a(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		a(request,response);
	}
	
	protected void a(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			request.setCharacterEncoding("utf-8");
			String sign=request.getParameter("sign");
			if(sign==null) {
				return ;
			}else if(sign.equals("login")) {//로그인 처리
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");
				
				
					String name=mDao.login(id,pw);
					
					if(name!=null) {
						//login ok
						RequestDispatcher disp=request.getRequestDispatcher("login_ok.jsp");
						request.setAttribute("name", name);
						disp.forward(request, response);
					}else {
						//login fail					
						RequestDispatcher disp=request.getRequestDispatcher("login_fail.jsp");					
						disp.forward(request, response);
					}
				
				
				
			}else if(sign.equals("memberInsert")) {//회원가입 처리
							
				String id=request.getParameter("id");
				String pw=request.getParameter("pw");
				String name=request.getParameter("name");
				String [] all_subject=request.getParameterValues("subject");
				
							
				Member m=new Member(id,pw,name,all_subject);
				
					mDao.memberInsert(m);				
					RequestDispatcher disp=request.getRequestDispatcher("memberInsert_ok.jsp");					
					disp.forward(request, response);
						
			}else if(sign.equals("listMembers")) {//모든 회원 보기 처리
				
					List<Member> list=mDao.listMembers();
					//ok				
					RequestDispatcher disp=request.getRequestDispatcher("listMembers_ok.jsp");					
					request.setAttribute("list", list);
					disp.forward(request, response);				
				
			}else if(sign.equals("memberDelete")) {
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

}








