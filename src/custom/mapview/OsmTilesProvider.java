package custom.mapview;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;


public class OsmTilesProvider implements FinishedDownloadCallback{
	private static final String TAG = "OSM_TilesProvider";
	private static final String OSM_URL = "tile.openstreetmap.org";
	final int maxThreadNumber;												//Max Number of Download Threads
	HashSet<String> RequestsList = new HashSet<String>();
	ExecutorService ThreadPool; 											// ThreadPool for all requests
	FinishedDownloadCallback handler;										// Callback once download is finished
	private int CounterServers =1;
	/**
	 * Constructor, creates an Open Street Map Tile Provider
	 * with the specified number of threads to be used to download tiles
	 * @param maxThreadNumber
	 * @param handler
	 */
	public OsmTilesProvider(int maxThreadNumber, FinishedDownloadCallback handler){
		this.maxThreadNumber = maxThreadNumber;
		ThreadPool = Executors.newFixedThreadPool(maxThreadNumber);
		this.handler = handler;
	}
	/**
	 * We Call this Method from OsmTilesProvider and request an specific Tile, each time
	 * this method is called, the requested tile will be added to a ThreadList and wait for
	 * execution.
	 * @param x Tile number in X
	 * @param y Tile number in Y
	 * @param z Zoom Value for Tile
	 */
	public void downloadTile(int x, int y, int z){
		// Get a valid Open Street Map URL
		String url = CreateURL4OSM(x, y, z);
		String ThreadURL;
		// Adds the requested Tile to our RequestsList
		synchronized (RequestsList){
			//Check if it wasn't requested before and the adds it
			if (RequestsList.contains(url) == false){
				RequestsList.add(url);
				// Create a new task and execute it in a separate thread

				switch(CounterServers){
				case 1:
					ThreadURL = getThreadURL(url,"a.");
					break;
				case 2:
					ThreadURL = getThreadURL(url,"b.");
					break;
				case 3:
					ThreadURL = getThreadURL(url,"c.");
					break;
				default:
					ThreadURL = getThreadURL(url,"");
					CounterServers =0;
					break;

				}
				TileDownloadThread myThread = new TileDownloadThread(ThreadURL, this, x, y, z);
				ThreadPool.execute(myThread);

			}
			CounterServers++;
		}
	}
	/**
	 * Creates an Open Street Map URL for a given X,Y,Z tile number
	 * @param x tile in X plane
	 * @param y tile in Y plane
	 * @param z tile in Zoom plane
	 * @return
	 */
	String CreateURL4OSM(int x, int y, int z){
		String formatedURL = OSM_URL + "/" + z + "/" + x + "/" + y + ".png";
		return formatedURL;
	}

	String getThreadURL(String url, String server){
		String formatedURLthread = "http://" + server  + url;
		return formatedURLthread;
	}

	/**
	 * Callback Called by the TileDownloadThread once it Run() finished
	 * reports the Status of the Thread after execution.
	 * @param myThread
	 */
	public synchronized void handleDownload(TileDownloadThread myThread){
		//Get the final Status of the thread
		int state = myThread.getThreadState();
		if (state == TileDownloadThread.TASK_FINISHED){
			// Pass the the thread to TileProvider
			if (handler != null) {
				handler.handleDownload(myThread);
				Log.d(TAG,"Thread finished");
			}
		}
		else if (state == TileDownloadThread.TASK_FAILED){
			Log.d(TAG,"Task Failed");
		}
		// We remove the Thread that was requested from our RequestsList
		synchronized (RequestsList){
			Log.d(TAG,"Removing from requestlist " + myThread.getMyUrl());
			RequestsList.remove(myThread.getMyUrl());
		}

	}
	

	/**
	 * Cancel all pending downloads and recreate a new empty ThreadPool
	 */
	public void cancelDownloads(){
		ThreadPool.shutdownNow();
		synchronized (RequestsList){
			RequestsList.clear();
		}
		ThreadPool = Executors.newFixedThreadPool(maxThreadNumber);
	}
}