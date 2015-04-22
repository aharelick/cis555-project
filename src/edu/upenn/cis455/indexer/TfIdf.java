package edu.upenn.cis455.indexer;

public class TfIdf extends Thread {
	
	Doc doc;
	
	public TfIdf(String url, String content) {
		doc = new Doc(url, content);
	}
	
	@Override
	public void run() {
		doc.parseDocument();
		
	}
}
