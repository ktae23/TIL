package web.controller;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/main")
public class MainServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String sign=request.getParameter("sign");
		if(sign==null) {
			return;
		}else if(sign.equals("login")) {
			String id=request.getParameter("id");
			// dao에 login 처리
			// ok
			HttpSession session = request.getSession();
			session.setAttribute("id", id);
			RequestDispatcher disp = request.getRequestDispatcher("login_ok.jsp");
			disp.forward(request, response);
			
		}else if(sign.equals("logout")) {
			HttpSession session=request.getSession();
			session.invalidate();
			RequestDispatcher disp = request.getRequestDispatcher("index.html");
			disp.forward(request, response);
		
		}else if(sign.equals("basketInsert")) {
			HttpSession session=request.getSession();
			ArrayList<String> list=(ArrayList<String>)session.getAttribute("basket");
			if(list==null) {
				list=new ArrayList<String>();
				session.setAttribute("basket", list);
			}
			String product=request.getParameter("product");
			list.add(product);
			
			RequestDispatcher disp = request.getRequestDispatcher("basketView.jsp");
			disp.forward(request, response);
		}
	}

}
