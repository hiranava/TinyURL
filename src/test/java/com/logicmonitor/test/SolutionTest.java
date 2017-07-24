/**
 * 
 */
package com.logicmonitor.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author Hiranava
 *
 */
public class SolutionTest {

	private static final String BASEPOSTPATH = "/tinyurl/logicmonitor/url/encode";
	private static final String BASEGETPATH = "/tinyurl/logicmonitor/url/decode?shorturl=";
	private static final String BASEBLACKLISTPATH = "/tinyurl/logicmonitor/url/blacklist";

	private static String BASICURL = "";
	
	
	@BeforeClass
	public static void setup() {
		BASICURL = readProperties();
	}
	
	
	/**
	 * The end to end green path scenario is tested in this function. First a long URL
	 * is sent to get back a short URL. The Short URL is sent to get the long URL. The 
	 * short URL is blacklisted. The short URL is then sent to get back the original URL 
	 * which returns an forbidden status. The short URL is removed from blacklist and 
	 * again original URL is requested which successfully returns the long URL now.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void allGreenScenario() 
			throws ClientProtocolException, IOException {

		
		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlname";
	
			CloseableHttpClient client = HttpClientBuilder.create().build();
			CloseableHttpClient getClient = HttpClientBuilder.create().build();
			CloseableHttpClient blacklistclient = HttpClientBuilder.create().build();
			String tinyUrl = null;
			try {
				HttpPost post = new HttpPost(url);
	
				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);
	
				CloseableHttpResponse response = client.execute(post);
				try {
					Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));;
					try {
						StringBuffer result = new StringBuffer();
						String line = "";
						while ((line = rd.readLine()) != null) {
							result.append(line);
						}
						tinyUrl = result.toString();
	
					} catch (IOException e) {
						// TODO: handle exception
					}finally{
						rd.close();
					}
	
				} finally {
					response.close();
				}
	
	
				//Sending the shortened URL to get back the long URL
				String getUrl = BASICURL+BASEGETPATH+tinyUrl;
	
				HttpGet getRequest = new HttpGet(getUrl);
				HttpResponse getResponse = getClient.execute(getRequest);
				Assert.assertEquals(Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
	
				//Blacklist the URL
				final String blackListUrl = BASICURL+BASEBLACKLISTPATH;
				final String shortURL = tinyUrl;
				final String keyName1 = "url";
				String keyName2 = "status";
	
	
				HttpPost blackListPost = new HttpPost(blackListUrl);
	
				// add header
				blackListPost.setHeader("Content-Type", "application/json");
				JSONObject blacklistJson = new JSONObject();
				blacklistJson.put(keyName1, shortURL); 
				blacklistJson.put(keyName2, true); 
				StringEntity blacklisturlParameters = new StringEntity(blacklistJson.toString());
				blackListPost.setEntity(blacklisturlParameters);
	
				HttpResponse blackListResponse = blacklistclient.execute(blackListPost);
				Assert.assertEquals(Status.OK.getStatusCode(), blackListResponse.getStatusLine().getStatusCode());
	
				//Try to get the long URL corresponding to the Blacklisted URL
				//Sending the shortened URL to get back the long URL
				getUrl = BASICURL+BASEGETPATH+tinyUrl;
				getClient = HttpClientBuilder.create().build();
				getRequest = new HttpGet(getUrl);
				getResponse = getClient.execute(getRequest);
				Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), getResponse.getStatusLine().getStatusCode());
	
				//Remove the URL from blacklist
	
				blacklistclient = HttpClientBuilder.create().build();
				blackListPost = new HttpPost(blackListUrl);
	
				// add header
				blackListPost.setHeader("Content-Type", "application/json");
				blacklistJson = new JSONObject();
				blacklistJson.put(keyName1, shortURL); 
				blacklistJson.put(keyName2, false); 
				blacklisturlParameters = new StringEntity(blacklistJson.toString());
				blackListPost.setEntity(blacklisturlParameters);
	
				blackListResponse = blacklistclient.execute(blackListPost);
				Assert.assertEquals(Status.OK.getStatusCode(), blackListResponse.getStatusLine().getStatusCode());
			}catch(HttpHostConnectException e){
				System.out.println("Server is not up");
			} finally {
				client.close();
				getClient.close();
				blacklistclient.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}


	/**
	 * To test scenario where blacklist URL is not found
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void blacklistURLNotFound() 
			throws ClientProtocolException, IOException {
		
		if(!BASICURL.equals("")){

			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL + BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlname";
	
			CloseableHttpClient client = HttpClientBuilder.create().build();
			CloseableHttpClient getClient = HttpClientBuilder.create().build();
			CloseableHttpClient blacklistclient = HttpClientBuilder.create().build();
	
			try {
				HttpPost post = new HttpPost(url);
	
				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);
	
				HttpResponse response = client.execute(post);
				Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
	
	
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));
	
				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
	
				String tinyUrl = result.toString();
	
				//Sending the shortened URL to get back the long URL
				String getUrl = BASICURL + BASEGETPATH +tinyUrl;
	
				HttpGet getRequest = new HttpGet(getUrl);
				HttpResponse getResponse = getClient.execute(getRequest);
				Assert.assertEquals(Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
	
				//Blacklist the URL
				final String blackListUrl = BASICURL + BASEBLACKLISTPATH;
				//Wrong URL to check not found scenario
				final String shortURL = tinyUrl+"a";
				final String keyName1 = "url";
				String keyName2 = "status";
	
	
				HttpPost blackListPost = new HttpPost(blackListUrl);
	
				// add header
				blackListPost.setHeader("Content-Type", "application/json");
				JSONObject blacklistJson = new JSONObject();
				blacklistJson.put(keyName1, shortURL); 
				blacklistJson.put(keyName2, true); 
				StringEntity blacklisturlParameters = new StringEntity(blacklistJson.toString());
				blackListPost.setEntity(blacklisturlParameters);
	
				HttpResponse blackListResponse = blacklistclient.execute(blackListPost);
				Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), blackListResponse.getStatusLine().getStatusCode());
			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Server is not up");
			}finally{
				client.close();
				getClient.close();
				blacklistclient.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}

	}


	/**
	 * To test scenario where blacklist URL request is malformed
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void blacklistURLIsMalformed() 
			throws ClientProtocolException, IOException {
		
		if(!BASICURL.equals("")){
			
			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL + BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlname";
	
			CloseableHttpClient client = HttpClientBuilder.create().build();
			CloseableHttpClient getClient = HttpClientBuilder.create().build();
			CloseableHttpClient blacklistclient = HttpClientBuilder.create().build();
			try {
				HttpPost post = new HttpPost(url);
	
				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);
	
				HttpResponse response = client.execute(post);
				Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
	
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));
	
				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
	
				String tinyUrl = result.toString();
	
				//Sending the shortened URL to get back the long URL
				String getUrl = BASICURL + BASEGETPATH +tinyUrl;
	
				HttpGet getRequest = new HttpGet(getUrl);
				HttpResponse getResponse = getClient.execute(getRequest);
				Assert.assertEquals(Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
	
				//Blacklist the URL
				final String blackListUrl = BASICURL+BASEBLACKLISTPATH;
				//Wrong URL to check not found scenario
				final String shortURL = tinyUrl;
				final String keyName1 = "url";
				String keyName2 = "status";
	
				HttpPost blackListPost = new HttpPost(blackListUrl);
	
				// add header
				blackListPost.setHeader("Content-Type", "application/json");
				JSONObject blacklistJson = new JSONObject();
				blacklistJson.put(keyName1, shortURL); 
				//A string is passed in status to check malformed request
				blacklistJson.put(keyName2, "truep"); 
				StringEntity blacklisturlParameters = new StringEntity(blacklistJson.toString());
				blackListPost.setEntity(blacklisturlParameters);
	
				HttpResponse blackListResponse = blacklistclient.execute(blackListPost);
				Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), blackListResponse.getStatusLine().getStatusCode());
			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				
				System.out.println("Server is not up");
			} finally {
				client.close();
				getClient.close();
				blacklistclient.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}

	}

	/**
	 * Test not found scenario of Decode
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void urlNotFoundInDecode() 
			throws ClientProtocolException, IOException {

		if(!BASICURL.equals("")){

			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlname";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			CloseableHttpClient getClient = HttpClientBuilder.create().build();

			try {
				HttpPost post = new HttpPost(url);

				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);

				HttpResponse response = client.execute(post);
				Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				String tinyUrl = result.toString();

				//Sending the shortened URL to get back the long URL
				String getUrl = BASICURL + BASEGETPATH +tinyUrl+"x";

				HttpGet getRequest = new HttpGet(getUrl);
				HttpResponse getResponse = getClient.execute(getRequest);
				Assert.assertEquals(Status.NOT_FOUND.getStatusCode(), getResponse.getStatusLine().getStatusCode());
			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Server is not up");
			}finally{
				client.close();
				getClient.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}

	}

	/**
	 * Test not found scenario of Decode
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void badRequestFormedInEncode() 
			throws ClientProtocolException, IOException {

		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlnam";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			try {
				HttpPost post = new HttpPost(url);

				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);

				CloseableHttpResponse response = client.execute(post);
				try {
					Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
				} finally {
					// TODO: handle finally clause
					response.close();
				}
			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Server is not up");

			}finally{
				client.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}

	/**
	 * Test an all green scenario
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void checkDuplicacy() 
			throws ClientProtocolException, IOException {


		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "https://google.com";
			final String keyName = "urlname";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			CloseableHttpClient clientSecond = HttpClientBuilder.create().build();
			try {
				HttpPost post = new HttpPost(url);

				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);

				HttpResponse response = client.execute(post);
				Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

				BufferedReader rd = new BufferedReader(
						new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}

				String tinyUrl = result.toString();

				HttpPost postSecond = new HttpPost(url);

				// add header
				postSecond.setHeader("Content-Type", "application/json");
				JSONObject jsonSecond = new JSONObject();
				jsonSecond.put(keyName, originalURL); 
				StringEntity urlParametersSecond = new StringEntity(jsonSecond.toString());
				postSecond.setEntity(urlParametersSecond);

				HttpResponse responseSecond = clientSecond.execute(postSecond);
				Assert.assertEquals(Status.CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

				BufferedReader rdSecond = new BufferedReader(
						new InputStreamReader(responseSecond.getEntity().getContent()));

				StringBuffer resultSecond = new StringBuffer();
				String lineSecond = "";
				while ((lineSecond = rdSecond.readLine()) != null) {
					resultSecond.append(lineSecond);
				}

				String tinyUrlSecond = resultSecond.toString();
				Assert.assertEquals(tinyUrl, tinyUrlSecond);
			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Server is not up");
			}finally{
				client.close();
				clientSecond.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}

	/**
	 * Check for empty URL in Encode field
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void checkBlankEncode() 
			throws ClientProtocolException, IOException {


		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for google.com, 
			//expecting a created status and the shortened url
			final String url =BASICURL+BASEPOSTPATH;
			final String originalURL = " ";
			final String keyName = "urlname";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			try {
				HttpPost post = new HttpPost(url);

				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);

				HttpResponse response = client.execute(post);
				Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());

			} catch (HttpHostConnectException e) {
				// TODO Auto-generated catch block
				System.out.println("Server is not up");
			}finally{
				client.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}
	
	/**
	 * Check for Empty Original URL
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void emptyOriginalUrlenScenario() 
			throws ClientProtocolException, IOException {

		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for empty url, 
			//expecting a bad request status 
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "";
			final String keyName = "urlname";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			
			try {
				HttpPost post = new HttpPost(url);
	
				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);
	
				CloseableHttpResponse response = client.execute(post);
				try {
					Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));;
					try {
						StringBuffer result = new StringBuffer();
						String line = "";
						while ((line = rd.readLine()) != null) {
							result.append(line);
						}
	
					} catch (IOException e) {
						// TODO: handle exception
					}finally{
						rd.close();
					}
	
				} finally {
					response.close();
				}
			}catch(HttpHostConnectException e){
				System.out.println("Server is not up");
			} finally {
				client.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}
	
	/**
	 * Check for invalid URL 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	@Test
	public void inValidOriginalUrlenScenario() 
			throws ClientProtocolException, IOException {

		if(!BASICURL.equals("")){
			//Sending a request to get tinyurl for invalid url, 
			//expecting a bad request status 
			final String url = BASICURL+BASEPOSTPATH;
			final String originalURL = "xyab";
			final String keyName = "urlname";

			CloseableHttpClient client = HttpClientBuilder.create().build();
			
			try {
				HttpPost post = new HttpPost(url);
	
				// add header
				post.setHeader("Content-Type", "application/json");
				JSONObject json = new JSONObject();
				json.put(keyName, originalURL); 
				StringEntity urlParameters = new StringEntity(json.toString());
				post.setEntity(urlParameters);
	
				CloseableHttpResponse response = client.execute(post);
				try {
					Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));;
					try {
						StringBuffer result = new StringBuffer();
						String line = "";
						while ((line = rd.readLine()) != null) {
							result.append(line);
						}
						
	
					} catch (IOException e) {
						// TODO: handle exception
					}finally{
						rd.close();
					}
	
				} finally {
					response.close();
				}
			}catch(HttpHostConnectException e){
				System.out.println("Server is not up");
			} finally {
				client.close();
			}
		}else{
			System.out.println("Please enter IPADDRESS:PORT_NUM(http://localhost:8080) in config.properties");
		}
	}

	/**
	 * Reads URL from properties file
	 * @return
	 */
	private static String readProperties(){
		Properties props = new Properties();
		String url=null;
		InputStream is = ClassLoader.getSystemResourceAsStream("config.properties");
		try {
			if(is!=null)
				props.load(is);

			url  = props.getProperty("url");
		}
		catch (IOException e) {
			// Handle exception here
		}
		return url;
	}
}