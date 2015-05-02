package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * This class stores the name of a S3 file along
 * with a boolean that is true if the entire file
 * has been indexed.
 * @author Corey Loman
 *
 */
@Entity
public class S3File {

	@PrimaryKey
	private String filename;
	private boolean wasIndexed;
	
	public S3File() {
		
	}
	
	public S3File(String fname) {
		filename = fname;
		wasIndexed = false;
	}
	
	public String getFileName() {
		return filename;
	}
	
	public boolean wasIndexed() {
		return wasIndexed;
	}
	
	public void finishedIndexing(boolean b) {
		wasIndexed = b;
	}
}

