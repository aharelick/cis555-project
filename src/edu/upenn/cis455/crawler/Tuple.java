package edu.upenn.cis455.crawler;

import java.util.Date;

public class Tuple implements Comparable<Tuple>{ 
	public final Date left; 
	public final String right;
	
	public Tuple(Date d, String s) { 
		left = d; 
		right = s; 
	}
	
	public Tuple(byte [] arr) {
		String s = new String(arr);
		left = new Date(Long.parseLong(s.split("~~~")[0]));
		right = s.split("~~~")[1];
	}

	@Override
	public int compareTo(Tuple arg0) {
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
		return Long.toString(left.getTime()) + "~~~" + right;
	}
	
	public byte[] toByteArray() {
		return this.toString().getBytes();
	}
} 