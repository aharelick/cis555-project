package edu.upenn.cis455.indexer;

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
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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
	
	public static void main(String[] args) {
		start();
	}
	
	public static void start() {
		// pulls from S3
		// adds new url -> docContent to the blocking queue
		S3Object object = extractObject();
		
		//parse what we pulled from S3 here
		// check to see if it's XML or HTML
		// create new page and add to Blocking Queue
		
		
		// after all urls and content have been read from S3,
		// check for PageRank results
		// put all PR results to database, mapping url to PageRank score
	}
	
	private static S3Object extractObject() {
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
                System.out.println(" - " + objectSummary.getKey() + "  " +
                                   "(size = " + objectSummary.getSize() + ")");
            }
            System.out.println();
		return null;
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
