package com.panoramio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.util.Log;

import com.threads.interfaces.HandleSearchResultPanoramioCallback;

public class SearchTaskPanoramio extends Thread{
	

	// CONSTANTS DEFINITION
	public final static int TASK_IN_EXECUTION = 0;
	public final static int TASK_FINISHED = 1;
	public final static int TASK_FAILED = 2;
	
	private String panoramioRESTurl="http://www.panoramio.com/map/get_panoramas.php?set=full&from=0&to=100";

	private static final String TAG = "Search Panoramio Thread "; 
	private int ThreadState = TASK_IN_EXECUTION;	//Current thread State
	private byte[] JSONresponse = null;						//Stores XML Response File
	private String requestURL;
	private String left;
	private String top;
	private String right;
	private String bottom;
	// Called when task finishes its work or when it fails.
	private final HandleSearchResultPanoramioCallback callback;
	
	
	public SearchTaskPanoramio( HandleSearchResultPanoramioCallback callback, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude){
		this.callback = callback;
		this.left = Double.toString(minLongitude);
		this.bottom = Double.toString(minLatitude);
		this.right = Double.toString(maxLongitude);
		this.top = Double.toString(maxLatitude);
		requestURL = getSearchRequestURL (left,bottom,right,top);
		
	}
	
	private String getSearchRequestURL(String minx, String miny, String maxx, String maxy){
		return panoramioRESTurl + "&minx=" + minx + "&miny" + miny +  "&maxx=" + maxx + "&maxy="+maxy 
								+ "&size=medium&mapfilter=false";
	}

	public void run(){
		try
		{
			Log.d(TAG,"Sending_REST_Pano " + requestURL );
			URL url = new URL(requestURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Time in milliseconds the task has for the request
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.connect();
			
			//Response Code send by WebServer
			int response = connection.getResponseCode();
			Log.i(TAG,"Response:" + response );
			//In case of page not found error
			if (response == 404){				
				ThreadState = TASK_FAILED;
				return;
			}
			//If response is OK then download the XML response
			if (response == 200){
				InputStream is = connection.getInputStream();
				byte[] buffer = new byte[1024 * 4];
				ByteArrayOutputStream out = new ByteArrayOutputStream();

				while (true){
					int read = is.read(buffer);
					if (read == -1){
						break;
					}
					out.write(buffer, 0, read);
				}

				out.flush();
				JSONresponse = out.toByteArray();
				out.close();
				
				ThreadState = TASK_FINISHED;
			}
		}
		catch (SocketTimeoutException e1)
		{
			Log.e(TAG,"Socket_Timeout_Exception:" + e1.toString());
			ThreadState = TASK_FAILED;
		}
		catch (MalformedURLException e2)
		{
			Log.e(TAG,"Mal_Formed_URL: " +  e2.toString());
			ThreadState = TASK_FAILED;
		}
		catch (IOException e3)
		{
			Log.e(TAG,"IO_Exception: " +  e3.toString());
			ThreadState = TASK_FAILED;
		}
		catch (Exception e4)
		{	
			Log.e(TAG,"General_Exception: " + e4.toString());
			ThreadState = TASK_FAILED;
		}
		finally
		{
			// Report the result by passing this task
			if (callback != null) {
				callback.handleSearchResultPan(this);;
			}
		}
	}
	/**
	 * Gets the current Thread State
	 * @return
	 */
	public int getThreadState() {
		return ThreadState;
	}

	public void setThreadState(int ThreadState) {
		this.ThreadState = ThreadState;
	}
	/**
	 * Returns the XML response for the Search request
	 * @return
	 */
	public byte[] getJSONresponse() {
		return JSONresponse;
	}
	

}
