package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
/**
 * Class used to store xml documents with their associated url in the database
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class XmlDoc {
	@PrimaryKey
	private String url;
	private String xmlDoc;
	
	public XmlDoc()
	{
	}
	public XmlDoc(String url, String doc)
	{
		this.url = url;
		this.xmlDoc = doc;
	}
	public void setDoc(String doc)
	{
		this.xmlDoc = doc;
	}
	public String getDoc()
	{
		return xmlDoc;
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
