package com.flickr;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import com.threads.interfaces.HandleGetInfoFlickrCallback;

import android.os.Handler;
import android.util.Log;

public class GetInfoThread extends Thread{


	// CONSTANTS DEFINITION
	public final static int TASK_IN_EXECUTION = 0;
	public final static int TASK_FINISHED = 1;
	public final static int TASK_FAILED = 2;
	public final static String Api_Key = "af132e1981a446a441e273025b302f64";

	private static final String TAG = "Get Info Task"; 
	private int ThreadState = TASK_IN_EXECUTION;	//Current thread State
	private byte[] XMLresponse = null;						//Stores XML Response File
	private String id;
	// Called when task finishes its work or when it fails.
	private String parameters;
	private String myUrl ="http://api.flickr.com/services/xmlrpc/";
	private HandleGetInfoFlickrCallback callback;
	private int index;
	private Handler mHandler;
	
	public GetInfoThread(String ID, HandleGetInfoFlickrCallback callback,int index, Handler handler){
		this.parameters = getRPCtoGetInfo(ID);
		this.id = ID;
		this.callback = callback;
		this.index = index;
		this.mHandler = handler;
	}

	public void run(){
		try
		{
			Log.i(TAG,"Sending XML-RPC: " + myUrl );
			URL url = new URL(myUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Time in milliseconds the task has for the request
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("Content-length",Integer.toString(parameters.length()));

			// Send post request
			connection.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(parameters);
			wr.flush();
			wr.close();

			//Response Code sent by WebServer
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
				XMLresponse = out.toByteArray();
				out.close();

				ThreadState = TASK_FINISHED;
			}
		}
		catch (SocketTimeoutException e1)
		{
			Log.e(TAG,"SocketTimeoutException" + e1.toString());
			ThreadState = TASK_FAILED;
		}
		catch (MalformedURLException e2)
		{
			Log.e(TAG,"Mal Formed URL" +  e2.toString());
			ThreadState = TASK_FAILED;
		}
		catch (IOException e3)
		{
			Log.e(TAG,"IO Exception" +  e3.toString());
			ThreadState = TASK_FAILED;
		}
		catch (Exception e4)
		{	
			Log.e(TAG,"General Exception" + e4.toString());
			ThreadState = TASK_FAILED;
		}		finally
		{
			// Report the result by passing this task
			if (callback != null) {
				callback.handleInfoResult(this);;
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
	 * Returns the XMLresponse for the Info Request
	 * @return
	 */
	public byte[] getXMLresponse() {
		return XMLresponse;
	}
	public String getID(){
		return this.id;
	}

	private String getRPCtoGetInfo(String photoID) {
		String XMLserialized;
		XMLserialized =
				"<methodCall>"+
						"<methodName>flickr.photos.getInfo</methodName>" +
						"<params>"+
						"<param>"+
						"<value>"+
						"<struct>"+
						"<member>"+
						"<name>api_key</name>"+
						"<value><string>"+Api_Key+"</string></value>"+
						"</member>"+
						"<member>"+
						"<name>photo_id</name>"+
						"<value><string>"+photoID+"</string></value>"+
						"</member>"+
						"<member>"+
						"<name>format</name>"+
						"<value><string>rest</string></value>"+
						"</member>"+
						"</struct>"+
						"</value>"+
						"</param>"+
						"</params>"+
						"</methodCall>";
		return XMLserialized;

	}

	public int getIndex() {
		return index;
	}

	public Handler getmHandler() {
		return mHandler;
	}
}
