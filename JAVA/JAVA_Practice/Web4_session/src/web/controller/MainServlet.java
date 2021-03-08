package web.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request,response);
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String jsonInfo=request.getParameter("jsonInfo");
		JSONParser jsonParser=new JSONParser();
		JSONObject jsonObject=null;
		System.out.println(jsonInfo);
		try {
			jsonObject = (JSONObject) jsonParser.parse(jsonInfo);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String sign=(String) jsonObject.get("sign");
		
		if(sign==null) {
			return ;			
		}else if(sign.equals("login")) {
//			String id=request.getParameter("id");
//			String pw=request.getParameter("pw");
			
			
			
			String id=(String) jsonObject.get("id");
			String pw=(String) jsonObject.get("pw");
/////////////////////////////////////////////////////
			//dao login
			//ok			
			HttpSession session=request.getSession();
			session.setAttribute("id", id);		
			
////////////////////////////////////////////////////////			
			JSONObject returnObject=new JSONObject();
			returnObject.put("id", id);
			returnObject.put("pw", pw);
			
			String returnString=returnObject.toJSONString();
			
			PrintWriter out=response.getWriter();
			out.append(returnString);
			
//			RequestDispatcher disp=request.getRequestDispatcher("login_ok.xml");
//			disp.forward(request, response);
		}else if(sign.equals("basketInsert")) {
			String product=request.getParameter("product");
			HttpSession session=request.getSession();
			ArrayList<String> list=(ArrayList<String>) session.getAttribute("basket");
			if(list==null) {
				list=new ArrayList<String>();
				session.setAttribute("basket", list);
			}
			list.add(product);
			
			RequestDispatcher disp=request.getRequestDispatcher("basketInsert_ok.jsp");
			disp.forward(request, response);
		}else if(sign.equals("logout")) {
			HttpSession session=request.getSession();
			session.invalidate();//세션 무효화
			RequestDispatcher disp=request.getRequestDispatcher("index.html");
			disp.forward(request, response);
		}
	}

}






