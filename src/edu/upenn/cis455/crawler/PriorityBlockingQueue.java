package edu.upenn.cis455.crawler;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Date;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
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
	public Tuple pull() throws UnsupportedEncodingException, InterruptedException
	{
		final DatabaseEntry key = new DatabaseEntry();
		final DatabaseEntry value = new DatabaseEntry();
		synchronized(queueDB)
		{
			final Cursor cursor = queueDB.openCursor(null, null);
			try {
				
				while(cursor.getFirst(key, value, LockMode.RMW)==OperationStatus.NOTFOUND)
					queueDB.wait();
				
				if(value.getData() == null)
				{
					System.out.println("value of data is null");
					return null;
				}
			
				Tuple result = new Tuple(key.getData());
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
		
	}
	public synchronized void push(Tuple urlAndDate, String urlValue)
	{
		final DatabaseEntry newKey = new DatabaseEntry(urlAndDate.toByteArray());
		final DatabaseEntry newValue = new DatabaseEntry(urlValue.getBytes());
		synchronized(queueDB)
		{
			queueDB.put(null, newKey, newValue);
			counter++;
			if(counter >= cacheSize)
			{
				queueDB.sync();
				queueDB.notify();
				counter = 0;
			}
		}
	}

}
