package edu.upenn.cis455.crawler;

import java.util.Date;

public class Tuple<X, Y> implements Comparable<Tuple<X,Y>>{ 
	public final X left; 
	public final Y right; 
	public Tuple(X x, Y y) { 
		left = x; 
		right = y; 
	}

	@Override
	public int compareTo(Tuple<X, Y> arg0) {
		Date first = (Date) this.left;
		Date second = (Date) arg0.left;
		if (first.getTime() < second.getTime()) {
			return -1;
		} else if (first.getTime() == second.getTime()) {
			return 0;
		} else {
			return 1; //the larger the time, the more recent
		}
	} 
	
	@Override
	public String toString() {
		return "<" + left.toString() + ", " + right + ">";
	}
	
	public byte[] toByteArray(Tuple<X,Y> t) {
		return t.toString().getBytes();
	}
} 