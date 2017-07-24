package com.logicmonitor.tinyurl;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.validator.UrlValidator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Hiranava
 *
 */
@Path("/url")

public class Solution {

	//All constant variables
	final static String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	final static String ALERT_MSG ="The URL doesn't exist!";
	final static String BLACKLISTED_ALERT_MSG = "This URL has been blacklisted!";
	final static String BAD_REQUEST_ALERT_MESSAGE = "The request was not proper";
	final static String TINY_URL = "http://tinyurl.com/";
	final static String EMPTY_URL_ALERT_MSG = "Empty URL!";
	final static String BLACKLIST_SUCCES_MSG = "Successfully blacklisted";
	final static String REMOVE_FROM_BLACKLIST_SUCCES_MSG = "Removed from blacklist";
	//HashMap<Key,LongURL>
	static private HashMap<String, LongURL> urlPairMap = new HashMap<String,LongURL>();
	//HashMap<LongURL, ShortURL to check duplicates
	static private HashMap<String,String> reverseMap = new HashMap<String,String>();
	Random rand = new Random();
	String key = getRand();


	/**
	 * GET request to decode short URL to get long URL
	 * @param short URL
	 * @return long URL with HTTP status code 200. If URL is blacklisted return Status as 
	 * forbidden with alert message. If URL is not found then return status as NOT_FOUND
	 * with meaningful alert message.
	 * @throws URISyntaxException 
	 */
	@GET
	@Path("/decode")
	public Response decode(@QueryParam("shorturl") String shortURL) throws URISyntaxException {


		LongURL url = urlPairMap.get(shortURL.replace(TINY_URL, ""));
		if(url==null){
			return Response.status(Status.NOT_FOUND).entity(ALERT_MSG).build();
		}

		if(url.isBlacklisted){
			return Response.status(Status.FORBIDDEN).entity(BLACKLISTED_ALERT_MSG).build();
		}else{

			return Response.status(Status.OK).entity(url.url).build();
		}


	}

	/**
	 * POST request to receive long URL and encode it to make the short URL
	 * @param longURLJSON
	 * @return encoded short URL if long URL is proper else send bad request
	 */
	@POST
	@Path("/encode")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response encode(String longURLJSON) {
		while (urlPairMap.containsKey(key)) {
			key = getRand();
		}
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(longURLJSON);
			JSONObject jsonObject = (JSONObject) obj;
			String longURL = (String) jsonObject.get("urlname");
			
			//check if URL is valid
			if(longURL==null || !urlValidator(longURL) ){
				return Response.status(Status.BAD_REQUEST).entity(BAD_REQUEST_ALERT_MESSAGE).build();

			}else if(longURL.trim().equals("")){
				return Response.status(Status.BAD_REQUEST).entity(EMPTY_URL_ALERT_MSG).build();
			}
			
			//check if URL is null
			if(reverseMap.get(longURL)!=null){
				return Response.status(Status.CREATED).entity(TINY_URL + reverseMap.get(longURL)).build();
			}
			urlPairMap.put(key, new LongURL(longURL));
			
			//to protect against duplicate values we store the longURL
			reverseMap.put(longURL, key);
			
			return Response.status(Status.CREATED).entity(TINY_URL + key).build();

		} catch (ParseException e) {
			return Response.status(Status.BAD_REQUEST).entity(BAD_REQUEST_ALERT_MESSAGE).build();

		}catch (ClassCastException e){
			return Response.status(Status.BAD_REQUEST).entity(BAD_REQUEST_ALERT_MESSAGE).build();
		}


	}


	/**
	 * The blacklist status of short URL is set according to the status variable.
	 * @param blackURLJSON - JSON file contains two parameters, URL(Short URL) and Status
	 * @return returns OK status if the URL is properly blacklisted
	 */
	@POST
	@Path("/blacklist")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response blackList(String blacklistURLJSON) {

		try {

			JSONParser parser = new JSONParser();
			Object obj = parser.parse(blacklistURLJSON);
			JSONObject jsonObject = (JSONObject) obj;
			String blackURL = (String) jsonObject.get("url");
			boolean status = (Boolean) jsonObject.get("status");

			LongURL longURL = urlPairMap.get(blackURL.replace(TINY_URL, ""));
			if(longURL==null){
				return Response.status(Status.NOT_FOUND).entity(ALERT_MSG).build();
			}
			
			//check if URL is already blacklisted 
			if(longURL.isBlacklisted == status && status == true){
				return Response.status(Status.OK).entity("Already blacklisted!").build();
			}else if(longURL.isBlacklisted == status && status == false){
				return Response.status(Status.OK).entity("Not present in blacklist!").build();
			}
			
			//set status true or false as sent by user
			longURL.isBlacklisted = status;
			
			urlPairMap.put(blackURL.replace(TINY_URL, ""), longURL);
			
			//Success scenario
			if(status == true){
				return Response.status(Status.OK).entity(BLACKLIST_SUCCES_MSG).build();
			}else{
				return Response.status(Status.OK).entity(REMOVE_FROM_BLACKLIST_SUCCES_MSG).build();
			}
			
			
		} catch (ParseException e) {
			return Response.status(Status.BAD_REQUEST).entity(BAD_REQUEST_ALERT_MESSAGE).build();

		}catch (ClassCastException e){
			return Response.status(Status.BAD_REQUEST).entity(BAD_REQUEST_ALERT_MESSAGE).build();
		}
	}

	/**
	 * Uses random function to get a character from the predefined character string
	 * containing all possible values
	 * @return encoded unique key
	 */
	public String getRand() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(CHARS.charAt(rand.nextInt(62)));
		}
		return sb.toString();
	}
	
	/**
	 * This function uses Apache URL validator to  validate URLs sent by user
	 * @param url
	 * @return boolean whether URL is valid or not
	 */
	private boolean urlValidator(String url){
		if(url.length() < 7){
			return false;
		}else if(url.substring(0, 7).equals("http://") || url.substring(0, 8).equals("https://")){
			//do nothing
		}else{
			url = "http://" + url;
		}
		String[] schemes = {"http","https"}; 
		UrlValidator urlValidator = new UrlValidator(schemes);
		if (urlValidator.isValid(url)) {
		   return true;
		} else {
		   return false;
		}
	}
}

