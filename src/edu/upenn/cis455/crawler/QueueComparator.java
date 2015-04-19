package edu.upenn.cis455.crawler;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

public class QueueComparator implements Comparator<Tuple<String, Date>>, Serializable {

	@Override
	public int compare(Tuple<String, Date> o1, Tuple<String, Date> o2) {
		return o1.compareTo(o2);
	}
}
