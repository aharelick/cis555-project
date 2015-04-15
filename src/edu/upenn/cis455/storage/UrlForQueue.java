package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class used to store each url in the queue in the database. The index is the index in the queue, lower being closer to first in the queue
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class UrlForQueue {
	@PrimaryKey
	private Long index;
	private String url;
	public UrlForQueue()
	{
		
	}
	public UrlForQueue(Long index, String url)
	{
		this.index = index;
		this.url = url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setIndex(Long index)
	{
		this.index = index;
	}
	public Long getIndex()
	{
		return index;
	}

}
