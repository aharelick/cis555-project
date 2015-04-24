package edu.upenn.cis455.indexer;

public class Page {

	private String url;
	private String content;
	private String type; // can be "HTML" or "XML"
	
	public Page(String u, String c, String t) {
		url = u;
		content = c;
		type = t;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getContent() {
		return content;
	}
	
	public String getType() {
		return type;
	}
	
}
