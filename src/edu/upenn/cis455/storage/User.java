package edu.upenn.cis455.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
/**
 * Class used to store username and password in the database
 * @author Kelsey Duncombe-Smith
 *
 */
@Entity
public class User {
	
	
	private String password;
	
	@PrimaryKey
	private String username;

	public User()
	{
		
	}
	public User(String user, String pass)
	{
		username = user;
		password = pass;
	}
	public String getUsername()
	{
		return username;
	}
	public String getPassword()
	{
		return password;
	}
}
