package custom.mapview;

import java.util.ArrayList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.util.LruCache;
import android.util.Log;

public class TilesProvider implements FinishedDownloadCallback {
	//Constant Definition
	private static final String TAG = "Tiles Provider";
	public final static  int CACHE_MAX_N_TILES = 100;
	
	private OsmTilesProvider myOsmTilesProvider;									  //OpenStreetMap Tiles Provider
	public Object tilesLock = new Object();									  		  //Lock Object
	private Handler myTileHandler;													  //Handler to notify externals
	final int cacheSize;
	private LruCache<String, Tile> mMemoryCache;
	
	/**
	 * Constructor
	 * Creates a OsmTilesProvider with a max number of threads equals 3
	 * @param myTileHandler is the handler to be notified when tiles are downloaded
	 */
	public TilesProvider(Handler myTileHandler){
		myOsmTilesProvider = new OsmTilesProvider(8, this);
		this.myTileHandler = myTileHandler;
		cacheSize = CACHE_MAX_N_TILES;
	    mMemoryCache = new LruCache<String, Tile>(cacheSize);
	}

	/**
	 * Update the Cache with the required Tiles for the Visible Area at an Specific Zoom
	 * @param rect
	 * @param zoom
	 */
	public void fetchTiles(Rect rect, int zoom){
		synchronized (tilesLock){
			int maxIndex = (int) Math.pow(2, zoom) - 1;					// Max tile index for x and y

			//Calculate Required Tiles for Rendering the Visible Area
			ArrayList<String> requiredTiles = new ArrayList<String>();			
			for (int x = rect.left; x <= rect.right; x++){
				if (x < 0 || x > maxIndex){ 
					continue;
				}
				for (int y = rect.top; y <= rect.bottom; y++){
					if (y < 0 || y > maxIndex) {
						continue;
					}
					requiredTiles.add(x + ":" + y + ":" + zoom);
				}
			}		
			// Download the requiredTiles
			for (String string : requiredTiles){
				int x = 0, y = 0;
				String[] nums = string.split(":");
				x = Integer.parseInt(nums[0]);
				y = Integer.parseInt(nums[1]);
				Log.i(TAG, "Try Download Tile " + x + ":" + y + ":" + zoom);
				if (getTileFromMemCache(x + ":" + y + ":" + zoom) == null){
					myOsmTilesProvider.downloadTile(x, y, zoom);
				}
			}
		}
	}
	
	public void preFetchTiles(Rect rect, int zoom, int SurroundingTiles){
		synchronized (tilesLock){
			int maxIndex = (int) Math.pow(2, zoom) - 1;					// Max tile index for x and y

			//Calculate Surrounding Tiles on the Neighborhood of the Visible Area
			ArrayList<String> requiredTiles = new ArrayList<String>();			
			for (int x = (rect.left-SurroundingTiles); x <= (rect.right+SurroundingTiles); x++){
				if (x >= rect.left && x < rect.right){ 
					x= rect.right;
					continue;
				}		
				if (x < 0 || x > maxIndex){ 
					continue;
				}
				for (int y = (rect.top-SurroundingTiles); y <= (rect.bottom+SurroundingTiles); y++){
					if (y >= rect.top && y < rect.bottom){ 
						y= rect.bottom;
						continue;
					}	
					if (y < 0 || y > maxIndex) {
						continue;
					}
					requiredTiles.add(x + ":" + y + ":" + zoom);
				}
			}		
			// Download the surrounding Tiles
			for (String string : requiredTiles){
				int x = 0, y = 0;
				String[] nums = string.split(":");
				x = Integer.parseInt(nums[0]);
				y = Integer.parseInt(nums[1]);
				Log.i(TAG, "Try Prefetch Tile " + x + ":" + y + ":" + zoom);
				if (getTileFromMemCache(x + ":" + y + ":" + zoom) == null){
					myOsmTilesProvider.downloadTile(x, y, zoom);
				}
			}
		}
	}


	@Override
	public synchronized void handleDownload(TileDownloadThread myThread){
		byte[] tile = myThread.getFile();
		int x = myThread.getX();
		int y = myThread.getY();
		int z = myThread.getZ();

		Bitmap bm = BitmapFactory.decodeByteArray(tile, 0, tile.length);
		Tile t = new Tile(x, y, z, bm);
		// Add the new tile to our tiles memory cache
		synchronized (tilesLock){
		    if (getTileFromMemCache(x + ":" + y + ":" + z) == null) {
		    	addTileToMemoryCache(x + ":" + y + ":" + z, t);
		    }
		}
		if (myTileHandler != null){
			myTileHandler.sendEmptyMessage(0);
		}
	}
	
	public void addTileToMemoryCache(String key, Tile tile) {
	    if (getTileFromMemCache(key) == null) {
	        mMemoryCache.put(key, tile);
	    }
	}

	public Tile getTileFromMemCache(String key) {
	    return mMemoryCache.get(key);
	}
	/**
	 * gets the Cache Hit Rate
	 * @return
	 */
	public int getCacheHitRate(){
		return mMemoryCache.hitCount();
	}
	/**
	 * gets the cache miss rate
	 * @return
	 */
	public int getCacheMissRate(){
		return mMemoryCache.missCount();
	}
	/**
	 * Cancel current downloads on the OSM tiles provider
	 */
	public void cancelDownloads(){
		myOsmTilesProvider.cancelDownloads();
	}
	

}