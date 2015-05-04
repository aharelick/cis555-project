package edu.upenn.cis455.indexer;

import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class DocInfo {
	@PrimaryKey
	private String url;
	private String title;
	private ArrayList<String> words;
	private String text;
	
	public DocInfo()
	{
		url = "";
		title = "Unknown";
		words = new ArrayList<String>();
		text = "";
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getTitle()
	{
		return title;
	}
	public void addWord(String word)
	{
		words.add(word);
	}
	public ArrayList<String> getAllWords()
	{
		return words;
	}
	public void setDocText(String docText)
	{
		this.text = docText;
	}
	public String getDocText()
	{
		return text;
	}
}
