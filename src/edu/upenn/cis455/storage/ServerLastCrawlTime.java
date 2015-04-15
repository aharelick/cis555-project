package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class used to store the last crawl time of each server with the associated serverUrl to determine crawlDelay requirements
 * @author cis455
 *
 */
@Entity
public class ServerLastCrawlTime {

	@PrimaryKey
	private String serverUrl;
	
	private long lastCrawlTime;
	public ServerLastCrawlTime()
	{
		
	}
	public ServerLastCrawlTime(String url, long crawlTime)
	{
		serverUrl = url;
		lastCrawlTime = crawlTime;
	}
	public long getLastCrawlTime()
	{
		return lastCrawlTime;
	}
}
