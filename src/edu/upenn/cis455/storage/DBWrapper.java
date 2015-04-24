package edu.upenn.cis455.storage;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

import edu.upenn.cis455.crawler.PriorityBlockingQueue;
import edu.upenn.cis455.crawler.QueueComparator;
import edu.upenn.cis455.crawler.RobotsTxtInfo;
import edu.upenn.cis455.crawler.Tuple;

/**
 * The wrapper for the Berkeley DB database using primary indexes and entities and one database to store all
 * things required for HW2
 * @author Kelsey Duncombe-Smith
 *
 */
public class DBWrapper {
	
	private static String envDirectory = null;
	
	private static Environment myEnv;
	private static EntityStore store;
	
	private static PrimaryIndex<String, User> userIndex;
	private static PrimaryIndex<String, RobotsTxtInfo> robotsIndex;
	private static PrimaryIndex<String, ServerFutureCrawlTime> serverFutureCrawlIndex;
	private static PrimaryIndex<String, DocumentLastCrawlTime> documentLastCrawlIndex;
	private static PrimaryIndex<String, Channel> channelIndex;
	private static PrimaryIndex<String, HtmlDoc> htmlDocsIndex;
	private static PrimaryIndex<String, XmlDoc> xmlDocsIndex;
	private static PrimaryIndex<Long, UrlForQueue> queueIndex;
	private static Database crawledDB;
	private static Database queueDB;
	private static PriorityBlockingQueue headQueue;
	private static PriorityBlockingQueue getQueue;
	//private static PrimaryIndex<String, UrlHasBeenCrawled> crawledIndex;
	/* TODO: write object store wrapper for BerkeleyDB */
	public DBWrapper(String dbdir){

		File dir = new File(dbdir);
		dir.mkdirs();
		
		 // Open the environment, creating one if it does not exist
		//Open the store, creating one if it does not exist
        EnvironmentConfig envConfig = new EnvironmentConfig();
        StoreConfig storeConfig = new StoreConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(false);
        
        myEnv = new Environment(dir,envConfig);
        System.out.println(dir.getAbsolutePath());
        store = new EntityStore(myEnv, "EntityStore", storeConfig);
        userIndex = store.getPrimaryIndex(String.class, User.class);
        robotsIndex = store.getPrimaryIndex(String.class, RobotsTxtInfo.class);
        serverFutureCrawlIndex = store.getPrimaryIndex(String.class, ServerFutureCrawlTime.class);
        documentLastCrawlIndex = store.getPrimaryIndex(String.class, DocumentLastCrawlTime.class);
        channelIndex = store.getPrimaryIndex(String.class, Channel.class);
        htmlDocsIndex = store.getPrimaryIndex(String.class, HtmlDoc.class);
        xmlDocsIndex = store.getPrimaryIndex(String.class, XmlDoc.class);
        queueIndex = store.getPrimaryIndex(Long.class, UrlForQueue.class);
        
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        crawledDB = myEnv.openDatabase(null, "crawledDB", dbConfig);
        //crawledIndex = store.getPrimaryIndex(String.class, UrlHasBeenCrawled.class);
        
        DatabaseConfig queueDBConfig = new DatabaseConfig();
        queueDBConfig.setAllowCreate(true);
        queueDBConfig.setTransactional(false);
        queueDBConfig.setDeferredWrite(true);
        queueDBConfig.setBtreeComparator(new QueueComparator());
        
        queueDB = myEnv.openDatabase(null,  "queueDB", queueDBConfig);
        headQueue = new PriorityBlockingQueue(queueDB, 0);
        getQueue = new PriorityBlockingQueue(queueDB, 0);
        
        //close database every time the program is shutdown
        DatabaseShutdownHook hook = new DatabaseShutdownHook(myEnv, store, crawledDB, queueDB);
        Runtime.getRuntime().addShutdownHook(hook);
        System.out.println("Database Started");
	}
	public static Tuple getNextOnHeadQueue() throws UnsupportedEncodingException, InterruptedException
	{
		Tuple dateAndUrl = headQueue.pull();
		long currentTime = System.currentTimeMillis();
		long futureCrawlTime = dateAndUrl.left.getTime();
		if(futureCrawlTime>currentTime)
		{
			System.out.println("cannot crawl head yet. sleeping for: "+(futureCrawlTime-currentTime));
			Thread.sleep(futureCrawlTime-currentTime);
		}
		return dateAndUrl;
	}
	public static void putOnHeadQueue(Tuple dateAndUrl, String urlValue)
	{
		headQueue.push(dateAndUrl, urlValue);
	}
	public static Tuple getNextOnGetQueue() throws UnsupportedEncodingException, InterruptedException
	{
		Tuple dateAndUrl = getQueue.pull();
		long currentTime = System.currentTimeMillis();
		long futureCrawlTime = dateAndUrl.left.getTime();
		if(futureCrawlTime>currentTime)
		{
			System.out.println("cannot crawl get yet. sleeping for: "+(futureCrawlTime-currentTime));
			Thread.sleep(futureCrawlTime-currentTime);
		}
		return dateAndUrl;
	}
	public static void putOnGetQueue(Tuple urlAndDate, String urlValue)
	{
		getQueue.push(urlAndDate, urlValue);
	}
	public static void closeQueueDB()
	{
		queueDB.close();
		crawledDB.close();
		myEnv.close();
	}
	public static void storeUser(User newUser)
	{
		userIndex.put(newUser);
	}
	public static User getUser(String uniqueUser)
	{
		return userIndex.get(uniqueUser);
	}
	public static void storeRobotsInfo(RobotsTxtInfo robotInfo)
	{
		robotsIndex.put(robotInfo);
	}
	public static RobotsTxtInfo getRobotsInfo(String serverUrl)
	{
		return robotsIndex.get(serverUrl);
	}
	public static void storeServerFutureCrawlTime(ServerFutureCrawlTime lastCrawlTime)
	{
		serverFutureCrawlIndex.put(lastCrawlTime);
	}
	public static ServerFutureCrawlTime getServerFutureCrawlTime(String serverUrl)
	{
		return serverFutureCrawlIndex.get(serverUrl);
	}
	public static void storeDocumentLastCrawlTime(DocumentLastCrawlTime lastCrawlTime)
	{
		documentLastCrawlIndex.put(lastCrawlTime);
	}
	public static DocumentLastCrawlTime getDocumentLastCrawlTime(String documentUrl)
	{
		return documentLastCrawlIndex.get(documentUrl);
	}
	public static void storeChannel(Channel channel)
	{
		channelIndex.put(channel);
	}
	public static Channel getChannel (String uniqueChannel)
	{
		return channelIndex.get(uniqueChannel);
	}
	public static void removeChannel(String uniqueChannel)
	{
		channelIndex.delete(uniqueChannel);
	}
	public static EntityCursor<Channel> getAllChannels ()
	{
		return channelIndex.entities();
	}
	public static void storeHtmlDoc(HtmlDoc doc)
	{
		htmlDocsIndex.put(doc);
	}
	public static HtmlDoc getHtmlDoc (String url)
	{
		return htmlDocsIndex.get(url);
	}
	public static void storeXmlDoc(XmlDoc doc)
	{
		xmlDocsIndex.put(doc);
	}
	public static XmlDoc getXmlDoc (String url)
	{
		return xmlDocsIndex.get(url);
	}
	/**
	 * stores the specified url in a UrlForQueue object in the queue primary index  
	 * @param newUrl
	 */
	public static void storeUrlForQueue(UrlForQueue newUrl)
	{
		String url = newUrl.getUrl();
		if(!url.startsWith("http"))
		{
			url = "http://"+url;
			newUrl.setUrl(url);
		}
		queueIndex.put(newUrl);
	}
	public static UrlForQueue removeUrlForQueue()
	{
		Long key = queueIndex.sortedMap().firstKey();
		UrlForQueue url = queueIndex.get(key);
		if(url!=null)
			queueIndex.delete(key);
		return url;
	}
	public static Long getLastIndex()
	{
		try{
			Long i = queueIndex.sortedMap().lastKey();
			if(i==null)
				return Long.valueOf(-1);
			else 
				return i;
		}catch(NoSuchElementException e)
		{
			return (long) -1;
		}
	}
	public static boolean isQueueEmpty()
	{
			return queueIndex.sortedMap().isEmpty();
	}
	public static boolean urlHasBeenCrawled(String url)
	{
		DatabaseEntry key = new DatabaseEntry("0".getBytes());
		DatabaseEntry data = new DatabaseEntry("1".getBytes());
		//System.out.println(crawledDB.get(null, key, data, null));
		if(OperationStatus.SUCCESS == crawledDB.get(null, key, data, null))
		{
			return true;
		}
		else
			return false;
	}
	public static void storeUrlHasBeenCrawled(String url)
	{
		DatabaseEntry key = new DatabaseEntry(url.getBytes());
		DatabaseEntry data = new DatabaseEntry("1".getBytes());
		crawledDB.put(null, key, data);
	}
	public static void clearUrlHasBeenCrawled()
	{
		/*EntityCursor<String> crawledUrls = crawledIndex.keys();
		ArrayList<String> allUrls = new ArrayList<String>();
		for(String url = crawledUrls.first(); url!=null; url = crawledUrls.next())
		{
			allUrls.add(url);
		}
		for(String url : allUrls)
		{
			crawledIndex.delete(url);
		}*/
		myEnv.removeDatabase(null, "crawledDB");
		
	}
	
}
