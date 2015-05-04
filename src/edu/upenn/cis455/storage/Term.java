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
	//maps a URL to the term frequency
	private HashMap<String, LinkedList<Integer>> locations;
	//maps a URL to a list of locations for a particular term
	
	//because Berkeley DB sucks
	public Term() {
	}
	
	public Term(String t) {
		term = t;
		tf = new HashMap<String, Double>();
		locations = new HashMap<String, LinkedList<Integer>>();
	}
	
	public void addFrequency(String url, double freq) {
		tf.put(url, freq);
	}
	
	public void addLocationList(String url, LinkedList<Integer> loc) {
		locations.put(url, loc);
	}
	
	public LinkedList<Integer> getLocations(String url) {
		return locations.get(url);
	}
	
	public double getTermFrequency(String url) {
		return tf.get(url);
	}
	public HashMap<String, Double> getUrlToTFHashMap()
	{
		return tf;
	}
	public String getTerm()
	{
		return term;
	}

}
