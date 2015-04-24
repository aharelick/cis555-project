package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class used to store the last crawl time of each server with the associated serverUrl to determine crawlDelay requirements
 * @author cis455
 *
 */
@Entity
public class ServerFutureCrawlTime {

	@PrimaryKey
	private String serverUrl;
	
	private long futureCrawlTime;
	public ServerFutureCrawlTime()
	{
		
	}
	public ServerFutureCrawlTime(String url, long crawlTime)
	{
		serverUrl = url;
		futureCrawlTime = crawlTime;
	}
	public long getFutureCrawlTime()
	{
		return futureCrawlTime;
	}
}
