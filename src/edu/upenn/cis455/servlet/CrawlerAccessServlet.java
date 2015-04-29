package edu.upenn.cis455.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.User;

public class CrawlerAccessServlet extends HttpServlet{
	@Override
		public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
			response.setContentType("text/html");
			response.sendRedirect("crawleraccess.html");
			
		}
		@Override
		public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			ServletContext context = getServletContext();
			String ip = request.getParameter("ipaddress");
			String port = request.getParameter("port");
			response.setContentType("text/html");
			//not sure if redirecting to a different server works. googled and it seems like it but will need to test
			response.sendRedirect("http://"+ip+":"+port);
		}
}
