package web.controller;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String sign=request.getParameter("sign");
		
		if(sign==null) {
			return;
		}else if(sign.equals("login")) {
			String id=request.getParameter("id");
			// dao로 login 수행
			// ok
			HttpSession session=request.getSession();
			System.out.println(id+"의 세션:"+session.getId());
			session.setAttribute("login_id", id);
			
			RequestDispatcher disp=request.getRequestDispatcher("login_ok.jsp");
			disp.forward(request, response);
			
		}else if(sign.equals("logout")) {
			HttpSession session = request.getSession(false);
			if(session!=null) {
				session.invalidate();
				RequestDispatcher disp=request.getRequestDispatcher("index.html");
				disp.forward(request, response);
			}
			
		}
	}

}
