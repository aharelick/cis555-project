package edu.upenn.cis455.crawler;

import java.io.Serializable;
import java.util.Comparator;

public class QueueComparator implements Comparator<byte[]>, Serializable {

	@Override
	public int compare(byte[] o1, byte[] o2) {
		// TODO Auto-generated method stub
		return new Tuple(o1).compareTo(new Tuple(o2));
	}
}
