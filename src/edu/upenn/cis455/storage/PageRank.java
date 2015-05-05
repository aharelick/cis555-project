package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * This class stores the url with the pagerank score
 * associated with it.
 * @author Corey Loman
 *
 */
@Entity
public class PageRank {

	@PrimaryKey
	private String url;
	private Double score;
	
	public PageRank() {
		
	}
	
	public PageRank(String url, Double score) {
		this.url = url;
		this.score = score;
	}
	
	public String getUrl() {
		return url;
	}
	
	public Double getPageRankScore() {
		return score;
	}
}
