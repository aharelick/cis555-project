package edu.upenn.cis455.storage;

import java.io.File;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

public class DBWrapperIndexer {
	
	private static Environment myEnv;
	private static EntityStore store;
	
	private static PrimaryIndex<String, Term> termIndex;
	private static PrimaryIndex<String, S3File> s3FileIndex;
	
	/**
	 * Create the DB if it doesn't exist and open it if it does exist.
	 * @param dbdir - the path to the database location
	 */
	
	public static void init(String dbdir) {

		File dir = new File(dbdir);
		boolean success = dir.mkdirs();
		if (success) {
			System.out.println("Created the database");
		}
		// Open the environment, creating one if it does not exist
		// Open the store, creating one if it does not exist
        EnvironmentConfig envConfig = new EnvironmentConfig();
        StoreConfig storeConfig = new StoreConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(false);
        
        myEnv = new Environment(dir,envConfig);
        System.out.println(dir.getAbsolutePath());
        store = new EntityStore(myEnv, "EntityStore", storeConfig);
        termIndex = store.getPrimaryIndex(String.class, Term.class);
        s3FileIndex = store.getPrimaryIndex(String.class, S3File.class);
        DatabaseShutdownHookIndexer hook = new DatabaseShutdownHookIndexer(myEnv, store);
        Runtime.getRuntime().addShutdownHook(hook);
        System.out.println("Database Started");
	}
	
	public static Term getTerm(String term) {
		return termIndex.get(term);
	}
	
	public static void putTerm(Term term) {
		termIndex.put(term);
	}
	
	public static void deleteTerm(String term) {
		termIndex.delete(term);
	}

	public static void sync() {
		//store.sync();
	}
	
	public static S3File getS3File(String filename) {
		return s3FileIndex.get(filename);
	}
	
	public static void putS3File(S3File s3File) {
		s3FileIndex.put(s3File);
	}
	
	public static void deleteS3File(String filename) {
		s3FileIndex.delete(filename);
	}
}
	
	
