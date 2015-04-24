package edu.upenn.cis455.indexer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import edu.upenn.cis455.storage.DBWrapperIndexer;
import edu.upenn.cis455.storage.Term;

public class Corpus {
	
	/**
	 * Takes in all the information for a specific crawled page and stores it in 
	 * the database.
	 * @param url - the url of the page crawled
	 * @param tf  - a map of terms to their term frequencies in this doc
	 * @param loc - a map of terms to a list of their locations in this doc
	 */
	public static void addDocInfo(String url, HashMap<String, Double> tf,
			HashMap<String, LinkedList<Integer>> loc) {
		
		Set<String> terms = tf.keySet();
		for (String term : terms) {
			Term dbTerm = DBWrapperIndexer.getTerm(term);
			double freq = tf.get(term);
			LinkedList<Integer> termLoc = loc.get(term);
			dbTerm.addFrequency(url, freq);
			dbTerm.addLocationList(url, termLoc);
			DBWrapperIndexer.putTerm(dbTerm);
		}
		
	}
	
}
