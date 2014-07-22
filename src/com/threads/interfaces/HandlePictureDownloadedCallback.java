package com.threads.interfaces;

import com.download.common.DownloadPictureThread;


public interface HandlePictureDownloadedCallback {
	
	public void handlePictureDownload(DownloadPictureThread myThread);

}
