package com.download.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import com.threads.interfaces.HandlePictureDownloadedCallback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

public class DownloadPictureThread implements Runnable  {

	// CONSTANTS DEFINITION
	public final static int TASK_IN_EXECUTION = 0;
	public final static int TASK_FINISHED = 1;
	public final static int TASK_FAILED = 2;
	private static final String TAG = "Download_Picture_Thread";

	private int ThreadState = TASK_IN_EXECUTION;	//Current thread State
	private final String myUrl; 					//URL from where Image is downloaded
	private HandlePictureDownloadedCallback callback;
	private Bitmap downloadedBMP;
	private String imageId;
	private searchResponseObject mPhotoInfo;
	private Handler handler;
	/**
	 * Constructor for flickr
	 * @param size
	 * @param callback
	 * @param photoInfo
	 */
	public DownloadPictureThread(String size, HandlePictureDownloadedCallback callback, searchResponseObject photoInfo, Handler handler){
		this.myUrl = photoInfo.getURLtoDownload(size);
		this.callback = callback;
		this.imageId = photoInfo.getId();
		this.mPhotoInfo = photoInfo;
		this.handler = handler;
	}

	/**
	 * Constructor for Panoramio
	 * @param callback
	 * @param photoInfo
	 */
	public DownloadPictureThread(HandlePictureDownloadedCallback callback, searchResponseObject photoInfo, Handler handler){
		this.myUrl = photoInfo.getURLtoDownloadPan();
		this.callback = callback;
		this.imageId = photoInfo.getId();
		this.mPhotoInfo = photoInfo;
		this.handler = handler;
	}

	public void run(){
		try
		{
			Log.i(TAG,"Dwnld Picture: " + myUrl );
			URL url = new URL(myUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			// Time in milliseconds the task has to download the Picture
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
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize =1 ;

				InputStream is = connection.getInputStream();
				downloadedBMP = BitmapFactory.decodeStream(is,null,options);
				is.close();
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
				callback.handlePictureDownload(this);
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

	/**
	 * Gets the ThreadState
	 * @param ThreadState
	 */
	public void setThreadState(int ThreadState) {
		this.ThreadState = ThreadState;
	}

	/**
	 * Returns the Downloaded Picture as Bitmap
	 * @return
	 */
	public Bitmap getBitmap() {
		return downloadedBMP;
	}

	/**
	 * Get the URL from which the Picture is downloaded
	 * @return
	 */
	public String getMyUrl() {
		return myUrl;
	}

	/**
	 * Get the image ID
	 * @return
	 */
	public String getImageId(){
		return this.imageId;
	}

	/**
	 * Get the PhotoInfo Object
	 * @return
	 */
	public searchResponseObject getmPhotoInfo() {
		return mPhotoInfo;
	}

	public Handler getHandler() {
		return handler;
	}
}
