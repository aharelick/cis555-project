package edu.upenn.cis455.indexer;

public class Page {

	private String url;
	private String content;
	
	public Page(String u, String c) {
		url = u;
		content = c;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getContent() {
		return content;
	}
	
}
