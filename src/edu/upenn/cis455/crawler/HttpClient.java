package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.DocumentLastCrawlTime;
/**
 * This client handles Https and http. Http is handled by manually sending a request and parsing the response.
 * By calling getDocument, a client will download the document specified in the constructor and return it as a string.
 * 
 * @author Kelsey Duncombe-Smith
 *
 */
public class HttpClient {
	private URL url;
	String contentType = "";

/**
 * initializes the string passed in into a URL to be used by other methods
 * @param urlString
 * @throws IOException
 */
	public HttpClient(String urlString) throws IOException
	{
		if(urlString!=null)
		{
		//System.out.println("url string is: "+urlString);
		String decodedUrlString = URLDecoder.decode(urlString, "UTF-8");
		//System.out.println("decoded url string is: "+decodedUrlString);
		String [] splitOnSlashes = decodedUrlString.split("/");
		String finalToken = splitOnSlashes[splitOnSlashes.length-1];
		if(!finalToken.endsWith("/")&&!finalToken.contains("."))
			decodedUrlString+="/";
		url = new URL(decodedUrlString);
		}
		
		
	}
	/**
	 * default constructor
	 */
	public HttpClient()
	{
		
	}
	/**
	 * sets up the connection for Https and then calls readInDocument to get the document as a string
	 * @returns the document as a string
	 * @throws IOException
	 */
	private String downloadViaHttps(URL currentUrl) throws IOException
	{
		HttpsURLConnection connection = (HttpsURLConnection)currentUrl.openConnection();
		connection.setRequestProperty("User-Agent:", "cis455crawler");
		connection.connect();
		connection.setReadTimeout(30000);
		String[] nosemicolons = connection.getContentType().split(";");
		contentType = nosemicolons[0];
		String docIn = readInDocument((InputStream)connection.getContent(), currentUrl);
		connection.disconnect();
		return docIn;
	}
	/**
	 * sets up the socket for http and calls readInDocument to download the document and 
	 * @returns the document as a string
	 * @throws IOException
	 */
	private String downloadViaHttp(URL currentUrl) throws IOException
	{
		Socket socket = new Socket(currentUrl.getHost(), 80);
		socket.setSoTimeout(20000);
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		//System.out.println("relative path = "+currentUrl.getPath());
		String request = "GET "+currentUrl+" HTTP/1.0\r\nUser-Agent: cis455crawler\r\n\r\n";
		//System.out.println(request);
		out.print(request);
		out.flush();
		//System.out.println("just wrote request to socket output stream");
		String docIn = readInDocument(socket.getInputStream(), currentUrl);
		socket.close();
		return docIn;
		
		
	}
	/**
	 * reads in the headers from an http request
	 * @param inStream
	 * @param currentUrl
	 * @returns a Hashmap of header name to list of header values as strings.
	 * @throws IOException
	 * @throws ParseException
	 */
	private HashMap<String, List<String>> readInHeaders(InputStream inStream, URL currentUrl) throws IOException, ParseException
	{
		HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
		String line = "";
		line = in.readLine();
		ArrayList<String> statusList = new ArrayList<String>();
		statusList.add(line);
		String previousHeader = null;
		headers.put("Status", statusList);
		while (!(line= in.readLine()).equals("")) 
		{
			
			//System.out.println("still reading headers: "+ line);
			String[] tokens = line.split(": ");
			//if there is no colon then this line is a continuation of the header 
			//from the previous line
			if(tokens.length==1)
			{
				if(previousHeader!=null)
				{
					//add the new field of the header to the list then put the key and list back in the hashmap
					ArrayList<String> previousHeaderList = (ArrayList<String>) headers.get(previousHeader);
					previousHeaderList.add(tokens[0]);
					headers.put(previousHeader, previousHeaderList);
				}
			}
			else if(tokens.length==2)
			{
				ArrayList<String> currentHeaderList = new ArrayList<String>();
				currentHeaderList.add(tokens[1].trim());
				headers.put(tokens[0].trim(), currentHeaderList);
				if(line.trim().startsWith("Content-Type"))
				{
					
					String[] nosemicolons = tokens[1].split(";");
					contentType = nosemicolons[0];
				}
				previousHeader = tokens[0].trim();
			}
			else
				throw new ParseException("more than two tokens for one header line",0);
		}
		return headers;
	}
	
	/**
	 * reads in the document from the specified InputStream. If it is http, parse the headers first and set the contentType
	 * 
	 * @param inStream
	 * @return
	 * @throws IOException
	 */
	private String readInDocument(InputStream inStream, URL currentUrl) throws IOException
	{
		//System.out.println("in readInDocument");
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
		//System.out.println("after BufferedReader");
		String fullDocument = "";
		String htmlHeaders="";
		String line = "";
		//line = in.readLine();
		if(isHttp(currentUrl))
		{
			while (!(line= in.readLine()).equals("")) {
				//System.out.println("still reading headers: "+ line);
				if(line.startsWith("Content-Type"))
				{
					
					String[] tokens = line.split(": ");
					String[] nosemicolons = tokens[1].split(";");
					contentType = nosemicolons[0];
				}
				htmlHeaders+=line;
				//line = in.readLine();
			}
		}
		while((line=in.readLine())!=null)
		{
			//System.out.println("still reading doc: "+ line);
			fullDocument+=line+"\n";
		}
		//System.out.println("after while loop");
		return fullDocument;
	}
	/**
	 * checks if the protocol is http
	 * @param currentUrl
	 * @returns true if it is http and false otherwise
	 */
	private boolean isHttp(URL currentUrl)
	{
		return currentUrl.getProtocol().equals("http");
	}
	/**
	 * checks if the protocol is https
	 * @param currentUrl
	 * @returns true if it is https and false otherwise
	 */
	private boolean isHttps(URL currentUrl)
	{
		return currentUrl.getProtocol().equals("https");
	}
	/**
	 * sends a HEAD request to the specified url
	 * @param currentUrl
	 * @returns a hashmap of header name to a list of header values all as strings
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws ParseException
	 */
	public HashMap<String, List<String>> sendHead(URL currentUrl) throws UnknownHostException, IOException, ParseException
	{
		if(isHttp(currentUrl))
		{
			Socket socket = new Socket(currentUrl.getHost(), 80);
			socket.setSoTimeout(20000);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			//System.out.println("relative path = "+currentUrl.getPath());
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			DocumentLastCrawlTime documentLastCrawlTime = DBWrapper.getDocumentLastCrawlTime(currentUrl.toString());
			
			String request = "HEAD "+currentUrl+" HTTP/1.0\r\nUser-Agent: cis455crawler\r\n";
			if(documentLastCrawlTime!=null)
			{
				String dateString = format.format(documentLastCrawlTime.getLastCrawlTime());
				request+="If-Modified-Since:"+dateString+"\r\n";
			}
			request+="\r\n";
			//System.out.println(request);
			out.print(request);
			out.flush();
			InputStream in = socket.getInputStream();
			
			HashMap<String, List<String>> headers = readInHeaders(in, currentUrl);
			socket.close();
			return headers;
			
		}
		else if(isHttps(currentUrl))
		{
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			DocumentLastCrawlTime documentLastCrawlTime = DBWrapper.getDocumentLastCrawlTime(currentUrl.toString());
			
			HttpsURLConnection connection = (HttpsURLConnection)currentUrl.openConnection();
			connection.setRequestProperty("User-Agent:", "cis455crawler");
			if(documentLastCrawlTime!=null)
			{
				String dateString = format.format(documentLastCrawlTime.getLastCrawlTime());
				//connection.setRequestProperty("If-Modified-Since", dateString);
			}
			
			connection.connect();
			connection.setReadTimeout(30000);
			HashMap<String, List<String>> headers = new HashMap<String, List<String>>();
			headers.putAll(connection.getHeaderFields());
			ArrayList<String> statusList = new ArrayList<String>();
			statusList.add(Integer.toString(connection.getResponseCode()));
			headers.put("Status", statusList);
			return headers;
		}
		else throw new ParseException("not an accepted protocol",0);
	}
	/**
	 * 
	 * @param urlString
	 * @returns the document as a string
	 * @throws IOException
	 */
	public String getDocument(String urlString) throws IOException
	{
		URL currentUrl = new URL(urlString);
		if(currentUrl.getProtocol().equals("https"))
			return downloadViaHttps(currentUrl);
		else if(currentUrl.getProtocol().equals("http"))
			return downloadViaHttp(currentUrl);
		else
			return null;
	}
	/**
	 * 
	 * @returns a string representing the content type of the response
	 */
	public String getContentType()
	{
		//System.out.println("content type is: "+contentType);
		return contentType;
	}
}
