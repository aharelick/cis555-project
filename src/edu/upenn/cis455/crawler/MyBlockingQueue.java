package edu.upenn.cis455.crawler;

import java.util.LinkedList;

public class MyBlockingQueue<E> {

	LinkedList<E> queue;
	final int capacity = 10000; //hard-coded capacity of 10000
	int queueSize = 0;
	boolean full = false;	
	boolean shutdown = false;
	
	public MyBlockingQueue() {
		queue = new LinkedList<E>();	
	}
	
	public void enqueue(E req) {
		synchronized(queue) {
			if (full) {
				System.out.println("The queue is full, temporarily drop req");
			} else {
				//add request to the end of the queue
				queue.add(req);
				//increment the size and check if it has reached capacity
				if (++queueSize == capacity) {
					full = true;
				}
				queue.notify();
			}
		}
	}
	
	public E dequeue() {
		synchronized(queue) {
			while (queueSize == 0) {
				try {
					queue.wait();
					/* This line allows for a graceful shutdown. After the 
					 * shutdown() function call, all the waiting threads are
					 * notified, and shutdown is set to true, so in the next
					 * line they will all return null, which is caught in the
					 * HttpServer class and exited appropriately.
					 */
					if (shutdown == true) {
						return null;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (--queueSize < capacity) {
				full = false;
			}
			return queue.remove();
		}
	}
	
	public void shutdown() {
		synchronized(queue) {
			queue.notifyAll();
			shutdown = true;
		}
	}
}