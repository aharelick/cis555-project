package edu.upenn.cis455.storage;

import com.sleepycat.je.Database;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityStore;
/**
 * A shutdown hook that closes the crawledDB, environment, and store anytime the program is shutdown
 * @author Kelsey Duncombe-Smith
 *
 */
public class DatabaseShutdownHook extends Thread{
	
	private Environment environment;
	private EntityStore store;
	private Database crawledDB;
	private Database queueDB;
	public DatabaseShutdownHook(Environment env, EntityStore store, Database crawledDB, Database queueDB)
	{
		this.environment = env;
		this.store = store;
		this.crawledDB = crawledDB;
		this.queueDB = queueDB;
	}
	public void run()
	{
		if(environment!=null)
		{
			crawledDB.close();
			queueDB.close();
			environment.removeDatabase(null, "crawledDB");
			environment.removeDatabase(null, "queueDB");
			store.close();
			environment.close();
			System.out.println("Closed the Database");
		}
	}

}
