package web.controller;

import java.io.IOException;
import java.io.PrintWriter;
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
		request.setCharacterEncoding("utf-8");
		String sign=request.getParameter("sign");
		if(sign==null) {
			return ;
		}else if(sign.equals("login")) {//로그인 처리
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			response.setContentType("text/html;charset=utf-8");
			PrintWriter out=response.getWriter();
			try {
				String name=mDao.login(id,pw);
				
				if(name!=null) {
					//login ok
					RequestDispatcher disp=request.getRequestDispatcher("login_ok.jsp");
					disp.forward(request,response);
				}else {
					//login fail
					out.write("다시 로그인 해주세요<br><a href='login.html' >다시 로그인 하기</a>");
				}
			} catch (MyException e) {
				// login error
				out.write(e.getMessage());
			}
			
			
		}else if(sign.equals("memberInsert")) {//회원가입 처리
			System.out.println(1);
			response.setContentType("text/html;charset=utf-8");
			PrintWriter out=response.getWriter();
			
			String id=request.getParameter("id");
			String pw=request.getParameter("pw");
			String name=request.getParameter("name");
			String [] all_subject=request.getParameterValues("subject");
			
						
			Member m=new Member(id,pw,name,all_subject);
			try {
				mDao.memberInsert(m);
				out.write("회원가입 되셨습니다");
			} catch (MyException e) {
				out.write(e.getMessage());
			}			
		}else if(sign.equals("listMembers")) {//모든 회원 보기 처리
			try {
				List<Member> list=mDao.listMembers();
				//ok
				response.setContentType("text/html;charset=utf-8");
				PrintWriter out=response.getWriter();

				out.append("<form action='main'>");
				out.append("<input type='hidden' name='sign' value='memberDelete'>");
				out.append("<input name='id'><input type='submit' value='회원 삭제'></form>");
				out.append("<table border='1'>");
				out.append("<tr><th>id</th><th>name</th>");
				for(Member m:list) {
					out.append("<tr><td>"+m.getId()+"</td><td>"+m.getName()+"</td></tr>");
				}
				out.append("</table>");


			} catch (MyException e) {
				// fail
				e.printStackTrace();
			} 
		} else if(sign.equals("memberDelete") ) {
			String id=request.getParameter("id");
			response.setContentType("text/html;charset=utf-8");
			PrintWriter out=response.getWriter();
			
			try {
				mDao.deleteMember(id);
				out.append(id + "님을 삭제했습니다.<br><a href='index.html'> 첫 화면으로 가기</a>");
			} catch (MyException e) {
				out.append(e.getMessage());
			}
		}
	}

}








