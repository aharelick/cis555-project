package edu.upenn.cis455.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
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
import edu.upenn.cis455.storage.Term;

//import edu.upenn.cis455.storage.Term;

/**
 * This class handles the main functionality for the search engine's indexer. It
 * pulls files from S3, parses them for documents and their content. It then
 * takes each individual document, adds its content to the inverted indexer,
 * calculates tf scores, and calculates the positioning of each word in the
 * document to be accessed from the front-end when returning results.
 * 
 * @author Corey Loman
 */
public class Indexer {

	/** Blocking Queue contains pages to be indexed. */
	private BlockingQueue<Page> pagesBQ;
	/** Blocking Queue contains files to be downloaded. */
	private BlockingQueue<S3Object> s3FilesBQ;

	/**
	 * Constructor that inits the blocking queue, opens the DB, and calls
	 * start().
	 */
	public Indexer() {
		// spins up all the DocIndexer threads
		pagesBQ = new LinkedBlockingQueue<Page>();
		s3FilesBQ = new LinkedBlockingQueue<S3Object>();
		// TODO fix with actual location as argument
		DBWrapperIndexer.init("/home/cis455/workspace/cis555-project/database");
		/*Term curr = DBWrapperIndexer.getTerm("privacy");
		System.out.println("curr is "+curr);
		Term current = DBWrapperIndexer.getTerm("past");
		System.out.println("curr is "+current);*/
		//System.out.println(curr.getTermFrequency("http://www.nytimes.com"));
		init();
	}

	private static int maxFiles;
	private static int numFiles;
	private static boolean keepRunning;
	protected static HashMap<S3Object, String> objectToFileName;
	private static String bucketName;

	/**
	 * used solely for testing the indexer
	 * 
	 * @param args
	 *            : command-line arguments
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: java indexer maxFilesToDownload bucketToDownloadFrom");
			System.out.println("maxFilesToDownload is an int. If you want no limit, input -1.");
			System.out.println("Bucket name should correspond to the S3 bucket to be pulling from, i.e. for.indexer");
			System.exit(0);
		}
		if (Integer.parseInt(args[0]) == -1) {
			maxFiles = Integer.MAX_VALUE;
		} else {
			maxFiles = Integer.parseInt(args[0]);
		}
		bucketName = args[1];
		keepRunning = true;
		numFiles = 0;
		objectToFileName = new HashMap<S3Object, String>();
		new Indexer();
	}

	/**
	 * Calls extractObject(), starts the DocIndexer threads, and handles moving
	 * PageRank results from S3 to EBS.
	 */
	public void init() {
		FileDownloader fd = new FileDownloader();
		fd.start();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		indexerThreads = new HashSet<DocIndexer>();
		for (int i = 0; i < 10; i++) {
			DocIndexer tmp = new DocIndexer();
			tmp.start();
			indexerThreads.add(tmp);
		}

		concatenatorThreads = new HashSet<Concatenator>();
		for (int i = 0; i < 4; i++) {
			Concatenator tmp = new Concatenator();
			tmp.start();
			concatenatorThreads.add(tmp);
		}

		// TODO after all urls and content have been read from S3,
		// check for PageRank results
		// put all PR results to database, mapping url to PageRank score
	}

	private HashSet<Concatenator> concatenatorThreads;
	private HashSet<DocIndexer> indexerThreads;

	/**
	 * An inner class that extends Thread. Each of these threads pulls off the
	 * blocking queue and starts indexing that document by calling
	 * parseDocument() on it.
	 * 
	 * @author Corey Loman
	 */
	class DocIndexer extends Thread {

		public DocIndexer() {

		}

		@Override
		public void run() {
			while (keepRunning) {
				Page curr = pagesBQ.poll();
				if (curr == null) {
					continue;
				}
				Doc doc = new Doc(curr.getUrl(), curr.getContent());
				doc.parseDocument();
			}
		}

	}

	/**
	 * An inner class that extends thread. Downloads the files from S3 so
	 * they're ready to parse
	 * 
	 * @author Corey Loman
	 */
	class FileDownloader extends Thread {

		public FileDownloader() {

		}

		@Override
		public void run() {
			while (keepRunning) {
				extractObject();
			}
		}

		private AmazonS3 s3;

		/**
		 * Pulls files from S3 that contain documents that need to be indexed.
		 */
		private void extractObject() {
			AWSCredentials credentials = null;
			try {
				credentials = new ProfileCredentialsProvider("default")
						.getCredentials();
			} catch (Exception e) {
				throw new AmazonClientException(
						"Cannot load the credentials from the credential profiles file. "
								+ "Please make sure that your credentials file is at the correct "
								+ "location (/home/cis455/.aws/credentials), and is in valid format.",
						e);
			}

			s3 = new AmazonS3Client(credentials);
			Region usEast1 = Region.getRegion(Regions.US_EAST_1);
			s3.setRegion(usEast1);

			//bucketName = "for.indexer"; //no longer needed. Now supplied as command-line argument
			System.out.println("Listing objects");
			ObjectListing objectListing = s3
					.listObjects(new ListObjectsRequest().withBucketName(
							bucketName).withPrefix("DocS3batch:"));
			for (S3ObjectSummary objectSummary : objectListing
					.getObjectSummaries()) {
				if (!keepRunning) break;
				numFiles += 1;
				if (numFiles > maxFiles) break;
				String key = objectSummary.getKey();
				System.out.println("Getting object: " + key);
				S3File tmp;
				if ((tmp = DBWrapperIndexer.getS3File(key)) != null) {
					if (!tmp.wasIndexed()) {
						indexFile(key);
						// TODO Fix this (Spot 3) - change to false
						tmp.finishedIndexing(true);
						DBWrapperIndexer.putS3File(tmp);
					}
				} else {
					tmp = new S3File(key);
					indexFile(key);
					// TODO Fix this (Spot 4) - change to false
					tmp.finishedIndexing(true);
					DBWrapperIndexer.putS3File(tmp);
				}
				// break; // for testing purposes only
			}
		}

		/**
		 * Indexes an individual file from S3. This file contains many
		 * documents.
		 * 
		 * @param key
		 *            : the key needed to pull the correct file from S3
		 */
		private void indexFile(String key) {
			S3Object object = s3
					.getObject(new GetObjectRequest(bucketName, key));
			System.out.println("About to concatenate");
			// TODO Fix this (spot #1)
			objectToFileName.put(object, key);
			s3FilesBQ.add(object);
		}
	}
	

	class Concatenator extends Thread {

		public Concatenator() {

		}

		boolean stopper;

		@Override
		public void run() {
			while (!stopper) {
				InputStream input = null;
				S3Object tmp = null;
				boolean keepChecking = true;
				while (keepChecking) {
					tmp = s3FilesBQ.poll();
					if (tmp != null) {
						keepChecking = false;
					}
				}
				if (numFiles > maxFiles) {
					keepRunning = false;
					for (Concatenator c : concatenatorThreads) {
						stopper = true;
						c.stopThread();
					}
				}
				try {
					input = tmp.getObjectContent();
					concatenateTextInputStream(input);
				} catch (IOException e) {
					e.printStackTrace();
				}
				input = null;
				// TODO Fix this (Spot #2)
				//String key = objectToFileName.get(input);
				//S3File tmp = DBWrapperIndexer.getS3File(key);
				//tmp.finishedIndexing(true);
				//DBWrapperIndexer.putS3File(tmp);
			}
		}

		private void stopThread() {
			stopper = true;
		}

		/**
		 * Splits a file into individual pages.
		 * Adds each page to the blocking queue incrementally.
		 * @param input
		 *            : an InputStream to read from the S3 file
		 * @throws IOException
		 */
		private void concatenateTextInputStream(InputStream input)
				throws IOException {
			String output = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				if (line.contains("CIS555###Split%%%Document***Line")) {
					if (line.trim().equals("CIS555###Split%%%Document***Line")) {
						String[] pageString = output.split("\t", 2);
						Page page = new Page(pageString[0], pageString[1]);
						pagesBQ.add(page);
						System.out.println("URL IS " + page.getUrl());
						output = "";
					} else {
						output += line
								.split("CIS555###Split%%%Document\\*\\*\\*Line")[0];
						String[] pageString = output.split("\t", 2);
						Page page = new Page(pageString[0], pageString[1]);
						pagesBQ.add(page);
						System.out.println("URL IS " + page.getUrl());
						output = line
								.split("CIS555###Split%%%Document\\*\\*\\*Line")[1];
					}
					// break; //comment this out for real use
					continue; // comment this in for real use
				}
				output += line;
			}
			System.out.println("Finished concatenating");
		}
	}

}
