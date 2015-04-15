package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Class used to store each html doc in the database with its associated url and full document as a string.
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class HtmlDoc {
	
	@PrimaryKey
	private String url;
	private String jsoupDoc;
	
	public HtmlDoc()
	{
	}
	public HtmlDoc(String url, String doc)
	{
		this.url = url;
		this.jsoupDoc = doc;
	}
	public void setDoc(String doc)
	{
		this.jsoupDoc = doc;
	}
	public String getDoc()
	{
		return jsoupDoc;
	}
	public void setUrlOfDoc(String url)
	{
		this.url = url;
	}
	public String getUrlOfDoc()
	{
		return url;
	}
}
