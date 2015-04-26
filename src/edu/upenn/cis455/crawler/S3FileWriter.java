package edu.upenn.cis455.crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class S3FileWriter {
	static File file;
	
	static FileWriter getFileWriter(File file) {
		try {
			return new FileWriter(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static void switchFileAndWriteToS3() {
		synchronized(file) {
			//File tempFile = 
		}
	}

}
