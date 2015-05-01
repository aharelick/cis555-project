package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import edu.upenn.cis455.crawler.RobotsTxtInfo;
import edu.upenn.cis455.crawler.HttpClient;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.DocumentLastCrawlTime;
import edu.upenn.cis455.storage.HtmlDoc;
import edu.upenn.cis455.storage.ServerFutureCrawlTime;
import edu.upenn.cis455.storage.UrlForQueue;
import edu.upenn.cis455.storage.XmlDoc;

/**
 * Run via main method, it has a starting URL and crawls all links to HTML 
 * and XML Documents reached from that start URL
 * @author Kelsey Duncombe-Smith, Mark Harding
 */
public class XPathCrawler {
	private static String startUrlString = "https://dbappserv.cis.upenn.edu/crawltest.html";
	private static String storePath;
	private static long maxSize;
	private static int maxNumFiles;
	private static int numCrawled = 0;
	private static HttpClient client;
	private static UrlForQueue firstUrlForQueue;
	private static HashMap<String, ArrayList<String>> urlToUrlList;
	private static boolean shutdown = false;
	private static File directory;
	private static int portNumber = 80;
	private static MyBlockingQueue<Socket> requestQueue = 
			new MyBlockingQueue<Socket>();
	
	
	/**
	 * returns the first item in the queue
	 * for testing purposes
	 * @return
	 */
	
	public static UrlForQueue getFirstItemInQueue()
	{
		return firstUrlForQueue;
	}
	
	/**
	 * @param currentUrl
	 * @returns a String representing the base url including port number of a url
	 */
	private static String getBaseUrl( URL currentUrl)
	{
		return currentUrl.getProtocol()+"://"+currentUrl.getAuthority();
	}
	
	/**
	 * Sends the HEAD request, if it's a 200 OK, downloads the robots.txt file,
	 * if it exists and parses it which stores it in the database.
	 * @param currentUrl
	 * @returns true if a robots.txt was downloaded and false otherwise
	 */
	private static boolean robotsTxtHandler(URL currentUrl)
	{
		String baseUrl = getBaseUrl(currentUrl);
		String robotsUrlString = baseUrl+"/"+"robots.txt";
		if(DBWrapper.getRobotsInfo(baseUrl)!=null)
		{
			return false;
		}
		URL robotsUrl = null;
		try {
			robotsUrl = new URL(robotsUrlString);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		HashMap<String, List<String>> headers;
		try {
			headers = client.sendHead(robotsUrl);
		} catch (ParseException e) {
			return false;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		String responseCode = headers.get("Status").get(0);
		if(responseCode.contains("200"))
		{
			System.out.println("ok to parse, 200 OK received");
			RobotsTxtInfo robotsInfo;
			try {
				robotsInfo = parseRobotsTxt(robotsUrlString);
			} catch (ParseException e) {
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				return true;
			}
			robotsInfo.setServerUrl(baseUrl);
			DBWrapper.storeRobotsInfo(robotsInfo);
		}
		else if(responseCode.contains("301")||responseCode.contains("302")) {
			System.out.println("should redirect probably");
			RobotsTxtInfo robotsInfo = new RobotsTxtInfo();
			robotsInfo.setServerUrl(baseUrl);
			DBWrapper.storeRobotsInfo(robotsInfo);
		} else {
			RobotsTxtInfo robotsInfo = new RobotsTxtInfo();
			robotsInfo.setServerUrl(baseUrl);
			DBWrapper.storeRobotsInfo(robotsInfo);	
		}
		return true;	
	}
	
	/**
	 * Parses the robots.txt according to the different keywords. Adding 
	 * all implemented keywords to the robotsTxtInfo object. 
	 * @param robotsUrl
	 * @returns the robotsTxtInfo object with all the keywords parsed from the robots.txt
	 * @throws IOException
	 * @throws ParseException
	 */
	private static RobotsTxtInfo parseRobotsTxt(String robotsUrl) 
			throws IOException, ParseException
	{
		String retrievedDocument = client.getDocument(robotsUrl).right;
		RobotsTxtInfo robotInfo = new RobotsTxtInfo();
		ArrayList<String> expectedKeywords = new ArrayList<String>();
		expectedKeywords.add("User-agent");
		expectedKeywords.add("User-Agent");
		expectedKeywords.add("user-agent");
		BufferedReader in = 
				new BufferedReader(new StringReader(retrievedDocument));
		String line = "";
		String currentUserAgent = null;
		while ((line=in.readLine())!=null)
		{
			System.out.println("line = "+line);
			if(line.trim().startsWith("#"))
				continue;
			String []tokens =line.split(": ");
			String keyword = tokens[0].trim();
			if(expectedKeywords.contains(keyword))
			{
				if(keyword.equals("User-agent")||keyword.equals("User-Agent")
						||keyword.equals("user-agent"))
				{
					String userAgent = tokens[1].trim();
					robotInfo.addUserAgent(userAgent);
					currentUserAgent = userAgent;
					expectedKeywords.clear();
					expectedKeywords.add("Disallow");
					expectedKeywords.add("Allow");
					expectedKeywords.add("Crawl-delay");
					expectedKeywords.add("Sitemap");
					expectedKeywords.add("Host");
					expectedKeywords.add("");
					
				}
				else if(keyword.equals("Disallow"))
				{
					if(currentUserAgent!=null)
						robotInfo.addDisallowedLink(currentUserAgent, tokens[1].trim());
					expectedKeywords.clear();
					expectedKeywords.add("Disallow");
					expectedKeywords.add("Allow");
					expectedKeywords.add("Crawl-delay");
					expectedKeywords.add("Sitemap");
					expectedKeywords.add("Host");
					//represents a new line b/c split will just make token[0]=""
					expectedKeywords.add("");
				}
				else if(keyword.equals("Allow"))
				{
					if(currentUserAgent!=null)
						robotInfo.addAllowedLink(currentUserAgent, tokens[1].trim());
					
					expectedKeywords.clear();
					expectedKeywords.add("Disallow");
					expectedKeywords.add("Allow");
					expectedKeywords.add("Crawl-delay");
					expectedKeywords.add("Sitemap");
					expectedKeywords.add("Host");
					//represents a new line b/c split will just make token[0]=""
					expectedKeywords.add("");
				
				}
				else if(keyword.equals("Crawl-delay"))
				{
					if(currentUserAgent!=null)
						robotInfo.addCrawlDelay(currentUserAgent, 
								Integer.parseInt(tokens[1].trim()));
					
					expectedKeywords.clear();
					expectedKeywords.add("Disallow");
					expectedKeywords.add("Allow");
					expectedKeywords.add("Crawl-delay");
					expectedKeywords.add("Sitemap");
					expectedKeywords.add("Host");
					//represents a new line b/c split will just make token[0]=""
					expectedKeywords.add("");
				
				}
				else if(keyword.equals(""))
				{
					currentUserAgent = null;
					expectedKeywords.clear();
					expectedKeywords.add("User-agent");
					expectedKeywords.add("User-Agent");
					expectedKeywords.add("user-agent");
					expectedKeywords.add("Sitemap");
					expectedKeywords.add("");
				}
			}
			else
				throw new ParseException("robots.txt, unexpected keyword: "+keyword,0);
		}
		
		robotInfo.print();
		return robotInfo;
	}
	
	/**
	 * Checks if the url is one of the urls specified by the robots.txt directives
	 * @param currentUrl
	 * @param robotsInfo
	 * @returns true if the url is allowed to be crawled, returns false otherwise
	 * @throws UnsupportedEncodingException
	 */
	private static boolean checkRobotsDirectives(URL currentUrl, 
			RobotsTxtInfo robotsInfo) throws UnsupportedEncodingException
	{
		boolean allowed = true;
		String matchedDisallowedPath = "";
		ArrayList<String> allowedPaths;
		ArrayList<String> disallowedPaths;
		
		if(robotsInfo.containsUserAgent("cis455crawler"))
		{
			allowedPaths = robotsInfo.getAllowedLinks("cis455crawler");
			disallowedPaths = robotsInfo.getDisallowedLinks("cis455crawler");
		}
		else if(robotsInfo.containsUserAgent("*"))
		{
			allowedPaths = robotsInfo.getAllowedLinks("*");
			disallowedPaths = robotsInfo.getDisallowedLinks("*");
		}
		else
		{//if no user-agent directives matching cis455crawler, url is allowed
			return true;
		}	
		char [] urlChars = currentUrl.getPath().toCharArray();
		if(disallowedPaths!=null)
		{
			for(String currentDisallowedPath : disallowedPaths)
			{
				char [] disallowedChars = currentDisallowedPath.toCharArray();
				if(doPathsMatch(disallowedChars, urlChars))
				{
					matchedDisallowedPath = currentDisallowedPath;
					allowed = false;
					break;
				}	
			}
		}
		if(allowedPaths!=null)
		{
			for(String currentAllowedPath : allowedPaths)
			{
				char [] allowedChars = currentAllowedPath.toCharArray();
				if(doPathsMatch(allowedChars, urlChars) && 
						(matchedDisallowedPath.length()<currentAllowedPath.length()))
				{
					/* The above condition works if there was no 
					 * matchedDisallowedPath because empty string is shorter 
					 * than any other string.
					 */
					allowed = true;
					break;
				}	
			}
		}
		return allowed;
	}
	
	/**
	 * Checks if the path in the robots.txt matches the current url
	 * parsing character by character.
	 * @param disallowedChars
	 * @param urlChars
	 * @returns true if the paths match and false otherwise
	 * @throws UnsupportedEncodingException
	 */
	private static boolean doPathsMatch(char [] disallowedChars,
			char [] urlChars) throws UnsupportedEncodingException
	{
		for(int i=0; i<disallowedChars.length; i++)
		{
			if(i==urlChars.length)
			{
				/*if urlCharArray shorter than the dissallowed path char array 
				then it is not a match*/
				return false;
			}
			
			if(disallowedChars[i]==urlChars[i])
			{
				continue;
			}
			else if(disallowedChars[i]=='%')
			{
				String encodedChar = Character.toString(disallowedChars[i]) +
						Character.toString(disallowedChars[i+1]) + 
						Character.toString(disallowedChars[i+2]);
				String decodedChar = URLDecoder.decode(encodedChar, "UTF-8");
				
				if(!decodedChar.equals('/'))
				{
					if(!decodedChar.equals(Character.toString(urlChars[i])))
						return false;
					else
						i+=2;
				}
				else{
					/*if the encoded %_ _ decoded to '/' then it cannot be 
					 * compared as a decoded string, and by this point if the %
					 * char matched the urlChar it would not have reached this
					 * point in the code because of the continue above*/
					return false;
				}
			}
			else
				return false;
		} 
		//if it matched all the way through the char array then it is a match
		return true;
	}
	
	/**
	 * Checks if the current file has a contentType that denotes an XML document
	 * @param contentType
	 * @param url
	 * @returns true if contentType is an accepted xml content-type and false otherwise
	 */
	private static boolean isXML(String contentType, URL url)
	{
		String [] tokens = contentType.split(";");
		contentType = tokens[0].trim();
		if(contentType.equals("application/xml")||contentType.equals("text/xml")||contentType.endsWith("+xml"))
			return true;
		if(url.toString().trim().endsWith(".xml"))
			return true;
		return false;
	}
	/**
	 * checks if the current file has a contentType that denotes an html document
	 * @param contentType
	 * @param url
	 * @returns true if contentType is an accepted html content-type and false otherwise
	 */
	private static boolean isHTML(String contentType, URL url)
	{
		if(contentType.equals("text/html")||url.toString().trim().endsWith(".html"))
			return true;
		else
			return false;
	}
	/**
	 * checks if the current file has a contentType that denotes an RSS document. redundant with xml because will be xml doc as well.
	 * @param contentType
	 * @param url
	 * @returns true if contentType is an accepted rss content-type and false otherwise
	 */
	private static boolean isRSS(String contentType, URL url)
	{
		if(contentType.equals("application/rss+xml")||url.toString().trim().endsWith(".rss"))
			return true;
		else
			return false;
	}
	/**
	 * sends a head request to the url, checks if that document has already been crawled this crawl, if the file has been modified
	 * since last crawl. Checks if the file is too large, or if the file is not an XML, HTML or RSS document and skips it if these are true.
	 * If the responseCode is 200 then it waits the crawl delay then downloads the document with a get request. If the document is an XML
	 * document then it compares the doc to each channel and evaluates it on each channels set of xpaths. If one of the xpaths for a channel
	 * matches then the xml is added to the channel.
	 * @param url
	 * @param crawlDelay
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	private static void headRequestHandler(URL url, int crawlDelay) 
			throws SAXException, ParserConfigurationException, IOException
	{
		//if the url has already been crawled this crawl then don't crawl it again
		if(DBWrapper.urlHasBeenCrawled(url.toString()))
		{
			System.out.println("URL has already been crawled this crawl. Skipping.");
			return;
		}
		//send the HEAD request
		HashMap<String, List<String>> headers = null;
		try {
			headers = client.sendHead(url);
		} catch (IOException e1) {
			e1.printStackTrace();
			return; 
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(headers == null)
			return;
		String responseCode = headers.get("Status").get(0);
		String contentType = "";
		if(responseCode.contains("200"))
		{ //only check content type of head if its a 200 0K
			if(headers.containsKey("Content-Type"))
			{
				contentType = headers.get("Content-Type").get(0).trim();
				String [] tokens = contentType.split(";");
				contentType = tokens[0].trim();
			}
			else
			{
				contentType = "";
			}
			//if the file is not XML, HTML, or RSS then do nothing with it
			if(!(isXML(contentType, url)||isHTML(contentType, url)||isRSS(contentType, url)))
			{
				return;
			}
			if(headers.containsKey("Content-Length"))
			{
				int contentLength = Integer.parseInt(headers.get("Content-Length").get(0));
				if(contentLength > maxSize)
				{
					System.out.println("error: file is too large. skipping.");
					return;
				}
			}
			else
			{
				System.out.println("error: no content length header. skipping.");
				return;
			}
		}
		XmlDoc xmlDoc=null;
		HtmlDoc htmlDoc=null;
		String retrievedDocument = "";
		if(responseCode.contains("304"))
		{
			System.out.println("304 not modified. Use local copy of "+url.toString());
			
			xmlDoc = DBWrapper.getXmlDoc(url.toString());
			if(xmlDoc!=null)
			{
				retrievedDocument = xmlDoc.getDoc();
			}
			htmlDoc = DBWrapper.getHtmlDoc(url.toString());
			if(htmlDoc!=null)
			{
				retrievedDocument = htmlDoc.getDoc();
			}
			if(retrievedDocument.isEmpty())
			{
				System.out.println("error no stored HTML or XML document with that url");
				return;
			}
			parseDocument(retrievedDocument, contentType, url, htmlDoc, xmlDoc);
			
		}
		else if(responseCode.contains("301")||responseCode.contains("302"))
		{
			if(headers.containsKey("Location"))
			{
				URL currentUrl = new URL(headers.get("Location").get(0));
				System.out.println("redirect, add to queue");
				addToHeadQueue(currentUrl);
			}
			else
			{
				System.out.println("No location header for redirect. skipping");
				return;
			}
			
		}
		else if(responseCode.contains("200"))
		{
			//ADD to GET QUEUE
			addToGetQueue(crawlDelay, url);
			System.out.println("File was a 200, adding URL to GET queue: "+url.toString());
		}
		else
		{
			System.out.println("File was not a 200, not downloading " 
		+url.toString()+". Response was: "+responseCode);
			return;
		}		
	}
	
	private static void addToGetQueue(int crawlDelay, URL currentUrl)
	{
		ServerFutureCrawlTime futureTime = DBWrapper.getServerFutureCrawlTime(
				currentUrl.getHost());
		//if not null, this host name has been crawled at least once before
		long futureTimeToCrawl = System.currentTimeMillis();
		if(futureTime != null)
		{
			futureTimeToCrawl = futureTime.getFutureCrawlTime();
			
			futureTimeToCrawl += crawlDelay*1000; 
		}
		System.out.println(futureTimeToCrawl);
		ServerFutureCrawlTime futureCrawlTime = new ServerFutureCrawlTime(currentUrl.getHost(), futureTimeToCrawl);
		DBWrapper.storeServerFutureCrawlTime(futureCrawlTime);
		
		Tuple dateAndUrl = new Tuple(new Date(futureTimeToCrawl), currentUrl.toString());
		DBWrapper.putOnGetQueue(dateAndUrl, currentUrl.toString());
		//store next future crawl time in DB
		
	}
	/**
	 * Adds the specified url to the head queue and updates the server future crawl time
	 * @param crawlDelay
	 * @param currentUrl
	 */
	private static void addToHeadQueue(URL currentUrl)
	{
		RobotsTxtInfo robotsInfo = DBWrapper.getRobotsInfo(getBaseUrl(currentUrl));
		int crawlDelay = getCrawlDelay(robotsInfo);
		ServerFutureCrawlTime futureTime = 
				DBWrapper.getServerFutureCrawlTime(currentUrl.getHost());
		
		//if not null, this host name has been crawled at least once before
		long futureTimeToCrawl = System.currentTimeMillis();
		if(futureTime != null)
		{
			futureTimeToCrawl = futureTime.getFutureCrawlTime();
			
			futureTimeToCrawl += crawlDelay*1000; 
		}
		//store next future crawl time in DB
		ServerFutureCrawlTime futureCrawlTime = new ServerFutureCrawlTime(
				currentUrl.getHost(), futureTimeToCrawl);
		DBWrapper.storeServerFutureCrawlTime(futureCrawlTime);
		
		Tuple dateAndUrl = new Tuple(new Date(futureTimeToCrawl), currentUrl.toString());
		DBWrapper.putOnHeadQueue(dateAndUrl, currentUrl.toString());	
	}
	
	/**
	 * Parses a downloaded or locally stored document and adds the links to the head queue
	 * @param retrievedDocument
	 * @param contentType
	 * @param url
	 * @param htmlDoc
	 * @param xmlDoc
	 */
	private static void parseDocument(String retrievedDocument,
			String contentType, URL url, HtmlDoc htmlDoc, XmlDoc xmlDoc)
	{
		Date currentDate = new Date();
		DocumentLastCrawlTime lastCrawlTime= new DocumentLastCrawlTime(
				url.toString(), currentDate);
		DBWrapper.storeDocumentLastCrawlTime(lastCrawlTime);
		numCrawled++;
		
		if(isHTML(contentType, url)||htmlDoc!=null)
		{
			org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(retrievedDocument);
			try {
				getLinksFromJsoupDoc(jsoupDoc, url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return;
			}
			HtmlDoc html = new HtmlDoc(url.toString(), retrievedDocument);
			DBWrapper.storeHtmlDoc(html);
			
		}
		
		/*if(isXML(contentType, url)||xmlDoc!=null)
		{
			//make document
			org.jsoup.nodes.Document doc = Jsoup.parse(retrievedDocument, "", Parser.xmlParser());
		}*/
		
		//store to mark as being crawled in this crawl
		DBWrapper.storeUrlHasBeenCrawled(url.toString());
	}
	
	/**
	 * Gets all links from an HTML document using the jsoup DOM Document.
	 * All links are added to the queue.
	 * @param doc
	 * @param currentUrl
	 * @param htmlDoc
	 * @throws MalformedURLException
	 */
	private static void getLinksFromJsoupDoc(org.jsoup.nodes.Document doc,
			URL currentUrl) throws MalformedURLException
	{
		Elements links = doc.select("a[href]");
		ArrayList<String> allLinks = new ArrayList<String>(); 
		for(Element link: links)
		{
			System.out.println("link: "+link.attr("href") + " link text: "+link.text());
			String linkpath = link.attr("href").trim();
			String absoluteUrl;
			if(linkpath.startsWith("http"))
				absoluteUrl = linkpath;
			else
			{
				String [] tokens = currentUrl.toString().trim().split("/");
				if(tokens[tokens.length-1].contains("."))
				{
					absoluteUrl = currentUrl.toString().substring(0,
							currentUrl.toString().lastIndexOf("/")) +
							(currentUrl.toString().endsWith("/")?"":"/")+linkpath;
				}
				else
					absoluteUrl= currentUrl.toString() +
					(currentUrl.toString().endsWith("/")?"":"/")+linkpath;
			}
			addToHeadQueue(new URL(absoluteUrl));
			allLinks.add(absoluteUrl);
		} 
		urlToUrlList.put(currentUrl.toString(), allLinks);
		String line = S3FileWriter.prepareFileLineUrlList(currentUrl.toString(), allLinks);
		S3FileWriter.writeToUrlFile(line);
		
	}
	/**
	 * Sets the initial url string
	 * @param url
	 * @throws MalformedURLException
	 */
	private static void setUrlString(String url) throws MalformedURLException
	{
		startUrlString = url;
	}
	
	/**
	 * Clears head and get queues completely
	 */
	private static void clearQueues()
	{
		
		try {
			DBWrapper.clearHeadQueue();
			DBWrapper.clearGetQueue();
		} catch (UnsupportedEncodingException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	private static void clearServerFutureCrawlTimeIndex()
	{
		while(!DBWrapper.isServerFutureCrawlTimeEmpty())
		{
			DBWrapper.removeServerFutureCrawlTime();
		}
	}
	

	public static void processHead(String url) throws MalformedURLException,
		UnsupportedEncodingException
	{
		if(url == null)
			return;
	
		URL currentUrl = new URL(url);
		//checks if robotsTxtInfo already stored, fetches if not, if not robots.txt, 
		//creates an empty RobotsTxtInfo and stores it 
		boolean downloaded = robotsTxtHandler(currentUrl);
		
		RobotsTxtInfo robotsInfo = DBWrapper.getRobotsInfo(getBaseUrl(currentUrl));
		boolean canCrawl = checkRobotsDirectives(currentUrl, robotsInfo);
		//robots.txt just downloaded so add back to head queue to send head request next time
		if(downloaded && canCrawl)
		{
			addToHeadQueue(currentUrl);
		}
		//robots.txt previously downloaded, so go ahead and crawl
		else if(!downloaded && canCrawl){
			//robots.txt was not downloaded because not the first time this host has been seen
			int crawlDelay = getCrawlDelay(robotsInfo);
			try {
				headRequestHandler(currentUrl, crawlDelay);
			} catch (SAXException | ParserConfigurationException | IOException e) {
				e.printStackTrace();
			}
			
		}
		else
		{
			System.out.println("can't crawl this url: "+ currentUrl);
		}
	}
	
	private static int getCrawlDelay(RobotsTxtInfo robotsInfo)
	{
		int crawlDelay = 1;
		if(robotsInfo!=null)
		{
		
			if(robotsInfo.crawlContainsUserAgent("cis455crawler"))
				crawlDelay = robotsInfo.getCrawlDelay("cis455crawler");
			else if(robotsInfo.crawlContainsUserAgent("*"))
				crawlDelay = robotsInfo.getCrawlDelay("*");
			else
				crawlDelay = 1;
		}
		System.out.println("XpathCrawler getCrawlDelay. crawl delay is: "+crawlDelay);
		return crawlDelay;
	}
	
	public static void processGet(String url) throws IOException
	{
		GenericTuple<String, String> tuple = client.getDocument(url);
		String contentType = tuple.left;
		String retrievedDocument = tuple.right;
		if (isHTML(contentType, new URL(url))) {
			String line = S3FileWriter.prepareFileLineDoc(url, retrievedDocument);
			S3FileWriter.writeToDocFile(line);
		}
		parseDocument(retrievedDocument, contentType, new URL(url), null, null);
	}
	
	/**
	 * The HeadThread class takes URLs from the headQueue, check for the
	 * robots.txt file if it hasn't been fetched already, and then sends 
	 * a HEAD request to determine if it should download the page.
	 */
	static class HeadThreadRunnable implements Runnable {	
    	public void run() {
    		while (!shutdown) { //this is to keep the thread alive and not return
        		String url = null;
				try {
					url = DBWrapper.getNextOnHeadQueue().right;
				} catch (UnsupportedEncodingException | InterruptedException e1) {
					e1.printStackTrace();
				}
        		//This line is necessary for the shutdown call, see PriorityBlockingQueue
        		if (url == null) {
        			break;
        		}
        		try {
					processHead(url);
				} catch (MalformedURLException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
	
	/**
	 * The GETThread class takes URLs from the GETqueue, downloads the page,
	 * if the page is HTML it extracts links, otherwise it checks XPaths and 
	 * updates channels if necessary.
	 */
	static class GetThreadRunnable implements Runnable {
		
		public void run() {
			while (!shutdown) { //this is to keep the thread alive and not return
				String url = null;
				try {
					url = DBWrapper.getNextOnGetQueue().right;
				} catch (UnsupportedEncodingException | InterruptedException e1) {
					e1.printStackTrace();
				}
				//This line is necessary for the shutdown call, see HandlerQueue
				if (url == null) {
					break;
				}
				try {
					processGet(url);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	static class S3WritingTask extends TimerTask {		
		public void run() {
			//every 10 minutes write to S3
			S3FileWriter.switchFileAndWriteToS3(directory);		
		}
	}
	
	static class ListenerRunnable implements Runnable {
		public void run() {
			try (
				ServerSocket serverSocket = new ServerSocket(portNumber, 1000);	
			) {
				while (true) {
					Socket clientSocket = serverSocket.accept();
					//take clientSocket and put it on the queue for worker threads
					if (shutdown) {
						System.out.println("Not accepting anymore requests");
					}
					requestQueue.enqueue(clientSocket);
				}		
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void requestToClearQueue() {
		System.out.println("clearing queue of links from previous crawl");
		clearQueues();
		clearServerFutureCrawlTimeIndex();
	}
	
	/**
	 * Takes in 4-5 command line arguments to initialize the crawler.
	 */
	public static void main(String[] args) throws UnknownHostException,
		IOException, ParseException
	{
		client = new HttpClient();
		
		if(args.length<4 || args.length>5)
		{
			System.out.println("incorrect number of command line arguments,"
					+ " expected 4 or 5, there were "+args.length);
			System.exit(-1);
		}
		try {
			setUrlString(args[0]);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		storePath = args[1];
		maxSize = Long.parseLong(args[2])*1000000;
		if(args.length==4)
			maxNumFiles = Integer.parseInt(args[3]);
		else
			maxNumFiles = -1;
		
		
		//set the filewriter for logging for S3 
		directory = new File(args[4]);
		S3FileWriter.setDocFileWriter(directory);
		S3FileWriter.setUrlFileWriter(directory);
		
		DBWrapper wrapper = new DBWrapper(storePath);
		addToHeadQueue(new URL("http://www.yahoo.com"));
		addToHeadQueue(new URL("http://www.wikipedia.org/Philosophy"));
		addToHeadQueue(new URL("http://www.reddit.com"));
		addToHeadQueue(new URL("http://www.yahoo.com"));
		addToHeadQueue(new URL("http://www.nytimes.com"));	
		
		urlToUrlList = new HashMap<String, ArrayList<String>>();
		
		//Create thread pools to run the crawler
		Thread[] headPool = new Thread[50];
		Thread[] getPool = new Thread[50];
		for (int i = 0; i < 1; i++) {
			headPool[i] = new Thread(new HeadThreadRunnable());
			headPool[i].start();
			getPool[i] = new Thread(new GetThreadRunnable());
			getPool[i].start();
		}

		TimerTask s3WritingTask = new S3WritingTask();
		Timer s3Handler = new Timer(true);
		//wait 5 seconds to start, try every 5 seconds
		s3Handler.scheduleAtFixedRate(s3WritingTask, 300000, 300000);
		
		//add a shutdown hook to properly close DB
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				DBWrapper.close();
				shutdown = true;
				System.out.println("Proper Shutdown.");
			}
		});
	}
}
