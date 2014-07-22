package com.download.common;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.threads.interfaces.HandlePictureDownloadedCallback;

import android.os.Handler;
import android.util.Log;


public class PictureThreadPool implements HandlePictureDownloadedCallback{

	private static final String TAG = "Picture Thread Pool";
	final int maxThreadNumber;
	ExecutorService ThreadPool;
	HandlePictureDownloadedCallback handler;
	HashSet<String> RequestsList = new HashSet<String>();
	private Handler mHandler;

	public PictureThreadPool(int maxThreadNumber, HandlePictureDownloadedCallback handler, Handler mHandler){
		this.maxThreadNumber = maxThreadNumber;
		ThreadPool = Executors.newFixedThreadPool(maxThreadNumber);
		this.handler = handler;
		this.mHandler = mHandler;
	}

	public void  downloadImage(String size,searchResponseObject photoInfo){

		synchronized (RequestsList){
			
			//Check if it wasn't requested before and the adds it
			if (RequestsList.contains(photoInfo.getURLtoDownload(size)) == false){
				RequestsList.add(photoInfo.getURLtoDownload(size));
				// Create a new task and execute it in a separate thread
				DownloadPictureThread myThread = new DownloadPictureThread(size, this, photoInfo, mHandler);
				ThreadPool.execute(myThread);

			}
		}

	}
	
	public void  downloadImagePan(searchResponseObject photoInfo){

		synchronized (RequestsList){
			
			//Check if it wasn't requested before and the adds it
			if (RequestsList.contains(photoInfo.getURLtoDownloadPan()) == false){
				RequestsList.add(photoInfo.getURLtoDownloadPan());
				// Create a new task and execute it in a separate thread
				DownloadPictureThread myThread = new DownloadPictureThread(this, photoInfo, mHandler);
				ThreadPool.execute(myThread);

			}
		}

	}
	
	
	@Override
	public synchronized void handlePictureDownload(DownloadPictureThread myThread) {
		// TODO Auto-generated method stub
		int state = myThread.getThreadState();
		if (state == DownloadPictureThread.TASK_FINISHED){
			if (handler != null) {
				handler.handlePictureDownload(myThread);
				Log.d(TAG,"Thread finished");
			}
		} else if(state == DownloadPictureThread.TASK_FINISHED){
			Log.d(TAG,"Task Failed");
		}
		// We remove the Thread that was requested from our RequestsList
		synchronized (RequestsList){
			Log.d(TAG,"Removing from requestlist " + myThread.getMyUrl());
			RequestsList.remove(myThread.getMyUrl());
		}
	}
	
	public void cancelDownloads(){
		ThreadPool.shutdownNow();
		synchronized (RequestsList){
			RequestsList.clear();
		}
		ThreadPool = Executors.newFixedThreadPool(maxThreadNumber);
	}


}
