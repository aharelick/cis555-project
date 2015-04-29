package edu.upenn.cis455.ui;

import java.io.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class IndexServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		RequestDispatcher requestDispatcher = request.getRequestDispatcher("/jsp/index.jsp");
		requestDispatcher.forward(request, response);
	}

	public void destroy() {
	
	}
}
