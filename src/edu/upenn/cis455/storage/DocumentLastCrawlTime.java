package edu.upenn.cis455.storage;

import java.util.Date;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
/**
 * Class stored in the database that keeps track of the last time a particular document was crawled as the Date when crawled.
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class DocumentLastCrawlTime {
	@PrimaryKey
	private String documentUrl;
	
	private Date lastCrawlTime;
	public DocumentLastCrawlTime()
	{
		
	}
	public DocumentLastCrawlTime(String url, Date crawlTime)
	{
		documentUrl = url;
		lastCrawlTime = crawlTime;
	}
	public Date getLastCrawlTime()
	{
		return lastCrawlTime;
	}
}
