package edu.upenn.cis455.indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;
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
import edu.upenn.cis455.storage.Term;

public class Indexer {
	
	private BlockingQueue<Page> bq;
	
	public Indexer() {
		// spins up all the DocIndexer threads
		bq = new LinkedBlockingQueue<Page>();
		// TODO fix with actual location as argument
		DBWrapperIndexer.init("/home/cis455/workspace/cis555-project/database");
		start();
	}
	
	public static void main(String[] args) {
		new Indexer();
		Term curr = DBWrapperIndexer.getTerm("ISIS");
		System.out.println(curr.getTermFrequency("http://www.nytimes.com"));
	}
	
	public void start() {
		// pulls from S3
		// adds new url -> docContent to the blocking queue
		extractObject();
		
		//parse what we pulled from S3 here
		// check to see if it's XML or HTML
		// create new page and add to Blocking Queue
		new DocIndexer().start();
		
		// after all urls and content have been read from S3,
		// check for PageRank results
		// put all PR results to database, mapping url to PageRank score
	}
	
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

	        AmazonS3 s3 = new AmazonS3Client(credentials);
	        Region usWest2 = Region.getRegion(Regions.US_EAST_1);
	        s3.setRegion(usWest2);

	        String bucketName = "for.indexer";
	        System.out.println("Listing objects");
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withPrefix("DocS3batch:"));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                String key = objectSummary.getKey();
                System.out.println("Getting object: " + key);
                S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
                ArrayList<String> pages = null;
                System.out.println("About to concatenate");
                try {
					pages = concatenateTextInputStream(object.getObjectContent());
				} catch (IOException e) {
					e.printStackTrace();
				}
                System.out.println("Finished concatenating");
                for (String p : pages) {
                	String[] splitted = p.split("\t", 2);
                	Page page = new Page(splitted[0], splitted[1]);
                	bq.add(page);
                	System.out.println("URL IS " + page.getUrl());
                	//System.out.println("CONTENT IS: " + page.getContent());
                	break;
                }
                System.out.println("Pages Downloaded: " + pages.size());
                break;
            }
	}
	
	private static ArrayList<String> concatenateTextInputStream(InputStream input) throws IOException {
        String output = "";
        ArrayList<String> splitted = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
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
        return splitted;
    }

	class DocIndexer extends Thread {
		
		public DocIndexer() {
			
		}
		
		@Override
		public void run() {
			// change this while loop statement probably
			// possible race condition?
			while (!bq.isEmpty()) {
				Page curr = bq.poll();
				Doc doc = new Doc(curr.getUrl(), curr.getContent());
				doc.parseDocument();
			}
		}
		
	}
	
}
