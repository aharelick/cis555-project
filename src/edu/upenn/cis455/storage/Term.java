package edu.upenn.cis455.storage;

import java.util.HashMap;
import java.util.LinkedList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
/**
 * The term entity class.
 * Will be used to store each term and it's tf and proximity scores.
 * Using the term as the primary key, the class has a map of 
 * URLs to term frequencies and a map of URLs to list of locations in
 * the respective document.
 */
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
		tf.put(url, freq);
	}
	
	public void addLocationList(String url, LinkedList<Integer> loc) {
		locations.put(url, loc);
	}

}
