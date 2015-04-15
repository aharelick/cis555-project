package edu.upenn.cis455.storage;

import java.util.HashMap;
import java.util.Set;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
/**
 * This class is stored in the DB and holds the channel name, xpaths of the channel, url of the stylesheet
 * XMLFiles that match the xpaths, and creator of the channel. All the methods are getters and setters of the fields.
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class Channel {
	
	@PrimaryKey
	private String name;
	private String[] xpaths;
	private String url;
	private HashMap<String, String> XMLFiles;
	private String creator;
	
	public Channel()
	{
		XMLFiles = new HashMap<String, String>();
	}
	public Channel(String name, String [] xpaths, String url, String creator)
	{
		this.name = name;
		this.xpaths = xpaths;
		this.url = url;
		XMLFiles = new HashMap<String, String>();
		this.creator = creator;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
	public void setXPaths(String [] paths)
	{
		xpaths = paths;
	}
	public String [] getXPaths()
	{
		return xpaths;
	}
	public void setStylesheetUrl(String url)
	{
		this.url = url;
	}
	public String getStylesheetUrl()
	{
		return url;
	}
	public void addXMLFile(String url, String document)
	{
		XMLFiles.put(url, document);
	}
	public String getXMLFile(String url)
	{
		return XMLFiles.get(url);
	}
	public Set<String> getAllXMLUrls()
	{
		return XMLFiles.keySet();
	}
	public boolean containsXMLFile(String url)
	{
		return XMLFiles.containsKey(url);
	}
	public void setCreator(String creator)
	{
		this.creator = creator;
	}
	public String getCreator()
	{
		return creator;
	}
	

}
