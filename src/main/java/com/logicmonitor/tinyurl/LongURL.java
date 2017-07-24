/**
 * 
 */
package com.logicmonitor.tinyurl;

/**
 * This class is an object of the long URL provided by the user and a boolean indicating whether the URL 
 * is blacklisted.
 * @author Hiranava
 *
 */
public class LongURL {
	String url;
	boolean isBlacklisted;

	/**
	 * Constructor for class. Sets longURL to url and blacklist status as false.
	 * @param longURL
	 */
	LongURL(String longURL){
		this.url = longURL;
		this.isBlacklisted = false;
	}

}
