package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class used to track if the UrlHasBeenCrawled in this crawl. used to prevent duplicate crawls in one crawl round
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class UrlHasBeenCrawled {
	@PrimaryKey
	private String url;
	public UrlHasBeenCrawled()
	{
		
	}
	public UrlHasBeenCrawled(String url)
	{
		this.url = url;
	}

}
