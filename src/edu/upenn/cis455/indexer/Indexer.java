package edu.upenn.cis455.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import edu.upenn.cis455.storage.DBWrapperIndexer;
import edu.upenn.cis455.storage.S3File;
//import edu.upenn.cis455.storage.Term;

/**
 * This class handles the main functionality for the search
 * engine's indexer. It pulls files from S3, parses them for
 * documents and their content. It then takes each individual
 * document, adds its content to the inverted indexer, calculates
 * tf scores, and calculates the positioning of each word in the
 * document to be accessed from the front-end when returning results.
 * @author Corey Loman
 */
public class Indexer {
	
	/** Blocking Queue contains pages to be indexed.*/
	private BlockingQueue<Page> pagesBQ;
	/** Blocking Queue contains files to be downloaded.*/
	private BlockingQueue<InputStream> s3FilesBQ;
	
	/**
	 * Constructor that inits the blocking queue, opens the DB,
	 * and calls start().
	 */
	public Indexer() {
		// spins up all the DocIndexer threads
		pagesBQ = new LinkedBlockingQueue<Page>();
		s3FilesBQ = new LinkedBlockingQueue<InputStream>();
		// TODO fix with actual location as argument
		DBWrapperIndexer.init("/home/cis455/workspace/cis555-project/database");
		start();
	}
	
	/**
	 *  used solely for testing the indexer
	 * @param args: command-line arguments
	 */
	public static void main(String[] args) {
		new Indexer();
		//Term curr = DBWrapperIndexer.getTerm("ISIS");
		//System.out.println(curr.getTermFrequency("http://www.nytimes.com"));
	}
	
	/**
	 * Calls extractObject(), starts the DocIndexer threads,
	 * and handles moving PageRank results from S3 to EBS.
	 */
	public void start() {
		// pulls from S3
		// adds new url -> docContent to the blocking queue
		FileDownloader fd = new FileDownloader();
		fd.start();
		System.out.println("HERE HERE HERE HERE");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//parse what we pulled from S3 here
		// check to see if it's XML or HTML
		// create new page and add to Blocking Queue
		HashSet<DocIndexer> indexerThreads = new HashSet<DocIndexer>();
		for (int i = 0; i < 10; i++) {
			DocIndexer tmp = new DocIndexer();
			tmp.start();
			indexerThreads.add(tmp);
		}
		
		HashSet<Concatenator> concatenatorThreads = new HashSet<Concatenator>();
		for (int i = 0; i < 4; i++) {
			Concatenator tmp = new Concatenator();
			tmp.start();
			concatenatorThreads.add(tmp);
		}
		
		// after all urls and content have been read from S3,
		// check for PageRank results
		// put all PR results to database, mapping url to PageRank score
	}
	

	/**
	 * An inner class that extends Thread. Each of these threads
	 * pulls off the blocking queue and starts indexing that
	 * document by calling parseDocument() on it.
	 * @author Corey Loman
	 */
	class DocIndexer extends Thread {
		
		public DocIndexer() {
			
		}
		
		@Override
		public void run() {
			// TODO change this while loop statement probably
			while (true) {
				Page curr = pagesBQ.poll();
				if (curr == null) {
					continue;
				}
				System.out.println("Bout to index");
				Doc doc = new Doc(curr.getUrl(), curr.getContent());
				doc.parseDocument();
			}
		}
		
	}
	
	/**
	 * An inner class that extends thread.
	 * Downloads the files from S3 so they're ready to parse
	 * @author Corey Loman
	 */
	class FileDownloader extends Thread {
		
		public FileDownloader() {
			
		}
		
		@Override
		public void run() {
			extractObject();
		}
		
		private AmazonS3 s3;
		private String bucketName;
		
		/**
		 * Pulls files from S3 that contain documents that need to
		 * be indexed.
		 */
		private void extractObject() {
			 AWSCredentials credentials = null;
		        try {
		            credentials = new ProfileCredentialsProvider("cis455_mark").getCredentials();
		        } catch (Exception e) {
		            throw new AmazonClientException(
		                    "Cannot load the credentials from the credential profiles file. " +
		                    "Please make sure that your credentials file is at the correct " +
		                    "location (/home/cis455/.aws/credentials), and is in valid format.",
		                    e);
		        }

		        s3 = new AmazonS3Client(credentials);
		        Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		        s3.setRegion(usEast1);

		        bucketName = "for.indexer";
		        System.out.println("Listing objects");
	            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
	                    .withBucketName(bucketName)
	                    .withPrefix("DocS3batch:"));
	            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
	                String key = objectSummary.getKey();
	                System.out.println("Getting object: " + key);
	                S3File tmp;
	                if ((tmp = DBWrapperIndexer.getS3File(key)) != null) {
	                	if (!tmp.wasIndexed()) {
	                		indexFile(key);
	                		tmp.finishedIndexing(true);
	                		DBWrapperIndexer.putS3File(tmp);
	                	}
	                } else {
	                	tmp = new S3File(key);
	                	indexFile(key);
	                	tmp.finishedIndexing(true);
	                	DBWrapperIndexer.putS3File(tmp);
	                }
	                //break;
	            }
		}
		
		/**
		 * Indexes an individual file from S3.
		 * This file contains many documents.
		 * @param key: the key needed to pull the correct file from S3
		 */
		private void indexFile(String key) {
	        S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
	        System.out.println("About to concatenate");
	        s3FilesBQ.add(object.getObjectContent());
		}
	}
		
	class Concatenator extends Thread {

		public Concatenator() {
			
		}

		@Override
		public void run() {
			InputStream input = null;
			boolean keepChecking = true;
			while (keepChecking) {
				input = s3FilesBQ.poll();
				if (input != null) {
					keepChecking = false;
				}
			}
			ArrayList<String> pages = null;
			try {
				pages = concatenateTextInputStream(input);
				System.out.println("returned");
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (String p : pages) {
				String[] splitted = p.split("\t", 2);
				Page page = new Page(splitted[0], splitted[1]);
				pagesBQ.add(page);
				System.out.println("URL IS " + page.getUrl());
				// System.out.println("CONTENT IS: " + page.getContent());
				// break;
			}
			input = null;
			System.out.println("Pages Downloaded: " + pages.size());

		}
				
			/**
			 * Splits a file into individual pages.
			 * @param input: an InputStream to read from the S3 file
			 * @return an array list where each element is a document's content
			 * @throws IOException
			 */
			private ArrayList<String> concatenateTextInputStream(InputStream input) throws IOException {
		        String output = "";
		        ArrayList<String> splitted = new ArrayList<String>();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		        while (true) {
		            String line = reader.readLine();
		            //System.out.println("LINE IS: " + line);
		            if (line == null) {
		            	System.out.println("LINE IS NULL");
		            	break;
		            }
		            if (line.contains("CIS555###Split%%%Document***Line")) {
		            	if (line.trim().equals("CIS555###Split%%%Document***Line")) {
		            		splitted.add(output);
		            		output = "";
						} else {
							output += line.split("CIS555###Split%%%Document\\*\\*\\*Line")[0];
							splitted.add(output);
							output = line.split("CIS555###Split%%%Document\\*\\*\\*Line")[1];
						}
		            	//break; //comment this out for real use
		            	continue; //comment this in for real use
		            }
		            output += line;
		        }
		        System.out.println("Finished concatenating");
		        return splitted;
		    }
		}
	
	
}
