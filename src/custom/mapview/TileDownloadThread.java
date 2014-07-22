package custom.mapview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import android.util.Log;



public class TileDownloadThread implements Runnable {

	// CONSTANTS DEFINITION
	public final static int TASK_IN_EXECUTION = 0;
	public final static int TASK_FINISHED = 1;
	public final static int TASK_FAILED = 2;
	private static final String TAG = "Tile Download Thread"; 


	private int ThreadState = TASK_IN_EXECUTION;	//Current thread State
	private byte[] file = null;						//Stores Downloaded File
	private final String myUrl; 					//URL from where Tiles are downloaded
	private final int x;						
	private final int y;
	private final int z;

	// Called when task finishes its work or when it fails.
	private final FinishedDownloadCallback callback;

	/**
	 * Constructor for TileDownloadThread
	 * @param url from where tiles are downloaded
	 * @param callback
	 * @param x coordinate in x of Tile to be downloaded
	 * @param y coordinate in y of Tile to be downloaded
	 * @param z	Zoom level of tile to be downloaded
	 */
	public TileDownloadThread(String url, FinishedDownloadCallback callback, int x, int y, int z){
		this.myUrl = url;
		this.callback = callback;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void run(){
		try
		{
			Log.e(TAG,"Downloading:" + myUrl );
			URL url = new URL(myUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Time in milliseconds the task has to download the tile
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
			//If response is OK then download the file
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
				file = out.toByteArray();
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
		}
		finally
		{
			// Report the result by passing this task
			if (callback != null) {
				callback.handleDownload(this);
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
	 * Returns the Downloaded Bitmap as file
	 * @return
	 */
	public byte[] getFile() {
		return file;
	}
	

	public String getMyUrl() {
			return RemoveServer(myUrl);
	}
	
	/**
	 * Removes the server name to be able to compare the thread url from the OSM provider
	 * @param Url
	 * @return
	 */
	public String RemoveServer(String Url) {
		String URL_id;
		URL_id = Url.replace("http://a.", "");
		URL_id = URL_id.replace("http://b.", "");
		URL_id = URL_id.replace("http://c.", "");
		URL_id = URL_id.replace("http://", "");
		return URL_id;
	}
	/**
	 * Gets thread X value
	 * @return
	 */
	public int getX() {
		return x;
	}
	/**
	 * Gets thread Y value
	 * @return
	 */
	public int getY() {
		return y;
	}
	/**
	 * Gets thread Z value
	 * @return
	 */
	public int getZ() {
		return z;
	}


}
