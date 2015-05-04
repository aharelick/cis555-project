package edu.upenn.cis455.indexer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class Doc {

	private String url;
	private String html;
	
	public Doc(String u, String h) {
		url = u;
		html = h;
		wordOccurrences = new HashMap<String,Integer>();
		locations = new HashMap<String, LinkedList<Integer>>();
	}
	
	/**
	 * Pools together all the words from a current document
	 * Currently sets the word to lower case and removes all extraneous
	 * characters, like . , ? ! 
	 * @param word
	 */
	private void addToInvertedIndex(String word) {
		// This is currently changing something like "Horses." to
		// "horses" and "carry-on" to "carryon"
		String basicWord = word.replaceAll("[^A-Za-z0-9]", "");
		basicWord = basicWord.toLowerCase();
		if (wordOccurrences.containsKey(basicWord)) {
			int tmp = wordOccurrences.get(basicWord);
			wordOccurrences.put(basicWord, tmp += 1);
		} else {
			wordOccurrences.put(basicWord, 1);
		}
	}
	
	/**
	 * When a document has been parsed, this pushes the local inverted
	 * index for that document to the DBWrapper.
	 */
	/*
	private void pushInvertedIndex() {
		Set<String> index = wordOccurrences.keySet();
		// TODO Put each word to the DBWrapper with value: url
	}
	*/
	
	/**
	 * maps a term to the number of times it occurs in this document
	 */
	private HashMap<String, Integer> wordOccurrences;
	
	public void parseDocument() {
		Document d = Jsoup.parse(html);
		String text = d.body().text();
		String[] tokens = text.split(" ");
		int i = 0;
		for (String t : tokens) {
			addToInvertedIndex(t);
			addLocation(t, i);
			i++;
		}
		//pushInvertedIndex();
		pushDocInfo();
	}
	

	private void addLocation(String t, int i) {
		if (locations.containsKey(t)) {
			LinkedList<Integer> tmp = locations.get(t);
			tmp.add(i);
			locations.put(t, tmp);
		} else {
			LinkedList<Integer> tmp = new LinkedList<Integer>();
			tmp.add(i);
			locations.put(t, tmp);
		}
	}
	
	/**
	 * maps a term to a list of locations it appears in this document
	 */
	private HashMap<String, LinkedList<Integer>> locations;

	private void pushDocInfo() {
		// put all the TF scores in the DB
		// DB has a term as the key that maps a url to that term's
		//frequency, check if contained in the hashmap first before putting
		HashMap<String,Double> scores = new HashMap<String,Double>();
		for (String term : wordOccurrences.keySet()) {
			if (term.equals("isis")) {
				System.out.println("We found ISIS and their TF is: " + getTermFrequency("ISIS"));
				System.out.println("ISIS can be found at: " + url);
			}
			scores.put(term, getTermFrequency(term));
		}
		Corpus.addDocInfo(url, scores, locations);
	}

	/**
	 * @return a set of all the words in the document
	 * that were stored in the inverted index
	 */
	public Set<String> getBagOfWords() {
		return wordOccurrences.keySet();
	}
	
	/**
	 * @param word that you want the TF score of
	 * @return an int representing the term frequency of a word
	 * on the given document
	 */
	public Double getTermFrequency(String word) {
		// might be an unnecessary line of code
		String basicWord = word.replaceAll("[^A-Za-z0-9]", "");
		return (new Double(wordOccurrences.get(basicWord))/new Double(wordOccurrences.keySet().size()));
	}
	
	public LinkedList<Integer> getTermLocations(String word) {
		return locations.get(word);
	}
	
}
