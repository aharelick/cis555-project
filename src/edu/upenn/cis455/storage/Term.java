package edu.upenn.cis455.storage;

import java.util.HashMap;
import java.util.LinkedList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class Term {
	
	@PrimaryKey
	private String term;
	private HashMap<String, Double> tf;
	private HashMap<String, LinkedList<Integer>> locations;
	
	public Term() {
		tf = new HashMap<String, Double>();
		locations = new HashMap<String, LinkedList<Integer>>();
	}
	
	public void addFrequency(String url, double freq) {
		
	}
	
	public void addLocation(String url, int loc) {
		
	}

}
