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
	private boolean head;
	/**
	 * Creates instance of queue
	 * 
	 * @param config queueDB	Database for queue
	 * @param dbConfig			Database Config for all of the Berkeley DB for the crawler
	 */
	public PriorityBlockingQueue(Database queueDatabase, final int cacheSize, boolean head)
	{
		queueDB = queueDatabase;
		counter = 0;
		this.cacheSize = cacheSize;
		this.head = head;
	}
	public boolean isEmpty()
	{
		final DatabaseEntry key = new DatabaseEntry();
		final DatabaseEntry value = new DatabaseEntry();
		final Cursor cursor = queueDB.openCursor(null, null);
		if(cursor.getFirst(key, value, LockMode.RMW)==OperationStatus.NOTFOUND)
		{
			cursor.close();
			return true;
		}
		else
		{
			cursor.close();
			return false;
		}
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
				System.out.println("In "+ (this.head?"HEAD:":"GET:")+" PULL key: left: "+result.left+" right: "+ result.right);
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

		System.out.println("in "+ (this.head?"HEAD:":"GET:")+" PUSH tuple = "+urlAndDate.left.toString()+" "+urlAndDate.right);

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
