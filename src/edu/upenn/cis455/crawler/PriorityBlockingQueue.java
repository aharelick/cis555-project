package edu.upenn.cis455.crawler;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
/**
 * adapted from http://sysgears.com/articles/lightweight-fast-persistent-queue-in-java-using-berkley-db/
 * @author Kelsey Duncombe-Smith
 *
 */
public class PriorityBlockingQueue {
	
	private final Database queueDB;
	private int counter;
	private int cacheSize;
	/**
	 * Creates instance of queue
	 * 
	 * @param config queueDB	Database for queue
	 * @param dbConfig			Database Config for all of the Berkeley DB for the crawler
	 */
	public PriorityBlockingQueue(Database queueDatabase, final int cacheSize)
	{
		queueDB = queueDatabase;
		counter = 0;
		this.cacheSize = cacheSize;
	}
	public String pull() throws UnsupportedEncodingException
	{
		final DatabaseEntry key = new DatabaseEntry();
		final DatabaseEntry value = new DatabaseEntry();
		final Cursor cursor = queueDB.openCursor(null, null);
		try {
			cursor.getFirst(key, value, LockMode.RMW);
			if(value.getData() == null)
				return null;
		
			final String result = new String(value.getData(), "UTF-8");
			cursor.delete();
			counter++;
			if(counter >= cacheSize)
			{
				queueDB.sync();
				counter = 0;
			}
			return result;
		} finally {
			cursor.close();
		}
		
	}
	public synchronized void push(Tuple<String, Date> urlAndDate, String urlValue)
	{
		final DatabaseEntry newKey = new DatabaseEntry(urlAndDate.toBytearray());
		final DatabaseEntry newValue = new DatabaseEntry(urlValue.getBytes());
		queueDB.put(null, newKey, newValue);
		counter++;
		if(counter >= cacheSize)
		{
			queueDB.sync();
			counter = 0;
		}	
	}

}
