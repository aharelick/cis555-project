package edu.upenn.cis455.storage;


import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityStore;

/**
 * A shutdown hook that closes the environment and store anytime the program is shutdown
 */
public class DatabaseShutdownHookIndexer extends Thread{
	
	private Environment environment;
	private EntityStore store;

	public DatabaseShutdownHookIndexer(Environment env, EntityStore store) {
		this.environment = env;
		this.store = store;
	}
	
	public void run() {
		if (environment != null) {
			store.sync();
			store.close();
			environment.close();
			System.out.println("Closed the Database");
		}
	}
}
