package edu.upenn.cis455.indexer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.upenn.cis455.storage.DBWrapperIndexer;

public class Indexer {
	
	private BlockingQueue<Page> bq;
	
	public Indexer() {
		// spins up all the DocIndexer threads
		bq = new LinkedBlockingQueue<Page>();
		// TODO fix with actual location as argument
		DBWrapperIndexer.init("");
		start();
	}
	
	public void start() {
		// pulls from S3
		// adds new url -> docContent to the blocking queue
		
		//parse what we pulled from S3 here
		// check to see if it's XML or HTML
		// create new page and add to Blocking Queue
		
		
		// after all urls and content have been read from S3,
		// check for PageRank results
		// put all PR results to database, mapping url to PageRank score
	}
	
	class DocIndexer extends Thread {
		
		public DocIndexer() {
			
		}
		
		@Override
		public void run() {
			// change this while loop statement probably
			while (true) {
				Page curr = bq.poll();
				Doc doc = new Doc(curr.getUrl(), curr.getContent(), curr.getType());
				doc.parseDocument();
			}
		}
		
	}
	
}
