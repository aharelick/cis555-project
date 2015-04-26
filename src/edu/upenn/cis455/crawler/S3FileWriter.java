package edu.upenn.cis455.crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class S3FileWriter {
	static File docFile;
	static File urlFile;
	static FileWriter docWriter;
	static FileWriter urlWriter;
	
	
	static void setDocFileWriter(File directory) {
		try {
			docFile = new File(directory +"/DocS3batch:" + System.currentTimeMillis() + ".txt");
			docWriter = new FileWriter(docFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	static void setUrlFileWriter(File directory) {
		try {
			urlFile = new File(directory +"/UrlS3batch:" + System.currentTimeMillis() + ".txt");
			urlWriter = new FileWriter(urlFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static synchronized void writeToDocFile(String line) {
		try {
			docWriter.append(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static synchronized void writeToUrlFile(String line) {
		try {
			urlWriter.append(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static String prepareFileLineDoc(String url, String doc) {
		return url +  "\t" + doc + "CIS555###Split%%%Document***Line";
	}
	
	static String prepareFileLineUrlList(String url, ArrayList<String> list) {
		return url +  "\t" + list.toString();
	}
	
	
	
	static void switchFileAndWriteToS3(File directory) {
		
		File urlFileToWrite;
		File docFileToWrite;
		
		synchronized(docWriter) {
			try {
				docWriter.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			docFileToWrite = new File(docFile.getAbsolutePath());
			setDocFileWriter(directory);
		} 
		
		synchronized(urlWriter) {
			try {
				urlWriter.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			urlFileToWrite = new File(urlFile.getAbsolutePath());
			setUrlFileWriter(directory);
		}

		//Grab the credentials for writing to S3
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (/home/cis455/.aws/credentials), and is in valid format.",
							e);
		}

		AmazonS3 s3 = new AmazonS3Client(credentials);
		Region usStandard = Region.getRegion(Regions.US_EAST_1);
		s3.setRegion(usStandard);

		String bucketName1 = "for.indexer";
		String key1 = docFileToWrite.getName();
		String bucketName2 = "for.indexer";
		String key2 = urlFileToWrite.getName();

		System.out.println("===========================================");
		System.out.println("Getting Started with Amazon S3");
		System.out.println("===========================================\n");

		System.out.println("Uploading a new object to S3 from a file\n");
		s3.putObject(new PutObjectRequest(bucketName1, key1, docFileToWrite));
		s3.putObject(new PutObjectRequest(bucketName2, key2, urlFileToWrite));
	}
}
