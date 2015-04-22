package edu.upenn.cis455.indexer;

import java.util.HashMap;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Doc {

	private String url;
	private String html;
	
	public Doc(String u, String h) {
		url = u;
		html = h;
		wordOccurrences = new HashMap<String,Integer>();
		parseDocument();
	}
	
	/**
	 * Pools together all the words from a current document
	 * Currently sets the word to lower case and removes all extraneous
	 * characters, like . , ? ! 
	 * @param word
	 */
	private void addToInvertedIndex(String word) {
		// This is currently changing something like "Horses." to
		// "horses" and "carry-on" becomes "carryon"
		String basicWord = word.replaceAll("[A-Za-z0-9", "");
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
	private void pushInvertedIndex() {
		Set<String> index = wordOccurrences.keySet();
		// TODO Put each word to the DBWrapper with value: url
	}
	
	private HashMap<String, Integer> wordOccurrences;
	
	private void parseDocument() {
		Document d = Jsoup.parse(html);
		String text = d.body().text();
		String[] tokens = text.split(" ");
		for (String t : tokens) {
			addToInvertedIndex(t);
		}
		pushInvertedIndex();
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
	public Integer getTermFrequency(String word) {
		// might be an unnecessary line of code
		String basicWord = word.replaceAll("[A-Za-z0-9", "");
		return wordOccurrences.get(basicWord);
	}
	
}
