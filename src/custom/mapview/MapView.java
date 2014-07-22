package custom.mapview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.location.Location;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MapView extends View{
	private static final String TAG = "MapView";					  //Tag for Debugging
	private ScaleGestureDetector mScaleDetector;					  //Detector for set a zoom in/out
	private GestureDetectorCompat mDetector; 						  //Detector for double touch and zoom in
	private float mScaleFactor = 1.f;								  //Scale factor for ScaleGestureDetector
	protected Context context;										  // Needed for View constructor
	protected int viewWidth, viewHeight;							  // MapView dimension in Pixels
	protected TilesProvider tileProvider;							  // our TileProvider
	protected TilesManager tileManager;								  // calculate the required Tiles
	protected PointDouble CenterViewLocation = new PointDouble(0, 0); // The location of the view center in longitude, latitude
	protected PointDouble lastTouchPos = new PointDouble(-1, -1);	  // Touch position values for panning/dragging
	protected Location MarkerLocation = null;						  // Location of the Marker
	protected boolean autoFollow = false;							  // If true then CenterViewLocation will always match MarkerLocation
	protected Bitmap positionMarker;								  // An image to draw at the phone's position
	private Rect visibleRegion;
	private Point CenterViewLocInPixels ;
	private Point offset;
	private boolean checkSides=true;

	// Different paints
	protected Paint fontPaint;
	protected Paint bitmapPaint = new Paint();
	protected Paint circlePaint = new Paint();
	
    private Paint compassColor = new Paint();
    private float angleCompass = 0;
	
	/**
	 * We create our MapView
	 * @param context for the super of view
	 * @param viewWidth  to assign width of the view
	 * @param viewHeight  to assign height of the view
	 * @param tilesProvider  define source of the tiles
	 * @param positionMarker image of the position marker
	 */
	public MapView(Context context, int viewWidth, int viewHeight, TilesProvider tilesProvider, Bitmap positionMarker){
		super(context);
		this.context = context;
		this.tileProvider = tilesProvider;
		this.viewWidth = viewWidth;
		this.viewHeight = viewHeight;
		this.positionMarker = positionMarker;
		tileManager = new TilesManager(viewWidth, viewHeight);	

		// Font paint is used to draw text
		fontPaint = new Paint();
		fontPaint.setColor(Color.DKGRAY);
		fontPaint.setShadowLayer(1, 1, 1, Color.BLACK);
		fontPaint.setTextSize(30);

		// Color of precision Circle
		circlePaint.setARGB(50, 9, 48, 222);
		circlePaint.setAntiAlias(true);
		setupCompass();

		fetchTiles();		// Fetching tiles from the tilesProvider
		calculateOffset();
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mDetector = new GestureDetectorCompat(context, new MyGestureListener());

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		setMeasuredDimension(viewWidth, viewHeight);
	}

	/**
	 * This method updates tilesManager to have the CenterViewLocation as the center of the view
	 * and get the visible tiles indexes from the visible region that tiles
	 * manager determined, finally tiles are fetched in Tile provider
	 * for the visible region and the zoom level and stored in the cache
	 */
	void fetchTiles(){
		Log.i(TAG, "Fetch" + CenterViewLocation.toString());
		tileManager.setLocation(CenterViewLocation.getX(), CenterViewLocation.getY());	// Update tilesManager to have the center of the view as this location
		visibleRegion = tileManager.getVisibleRegion();									// Get the visible tiles indexes as a Rect (left, top, right, bottom)
		Log.i(TAG, "visibleRegion" + visibleRegion.toString());
		tileProvider.fetchTiles(visibleRegion, tileManager.getZoom());					// Ask the tileProvider to request the Needed Tiles and put them on Cache
		tileProvider.preFetchTiles(visibleRegion, tileManager.getZoom(),2);				// Pre-fetches tiles from the surroundings.. two tile per side...
	}
	
	/**
	 * Calculates the offset required from the center of the view to the TopLeft corner on the screen
	 */
	void calculateOffset(){
		PointDouble CenterViewLocRatio = TilesManager.calcRatio(CenterViewLocation.getX(), CenterViewLocation.getY());
		int mapWidthInPixels = tileManager.getMapSizeInTiles() * TilesManager.TILE_SIZE;			 	//Get Map Size in Pixels
		CenterViewLocInPixels = new Point((int) (CenterViewLocRatio.getX() * mapWidthInPixels),			//CenterViewLocation position in Pixels for the current zoom of tileManager
										  (int) (CenterViewLocRatio.getY() * mapWidthInPixels));
		offset = new Point( (CenterViewLocInPixels.x - viewWidth  / 2),			
							(CenterViewLocInPixels.y - viewHeight / 2));	                        	// Calculate Offset to be used
	}


	/**
	 * This method is called always after an Invalidate on the view
	 */
	@Override
	protected void onDraw(Canvas canvas){
		canvas.drawARGB(255, 180, 180, 180);						// Set up BackgroundColor: alpha, red, green, blue
		drawTiles(canvas, offset);									// DrawTiles with an Specified Offset
		if (autoFollow==true){
			drawCompass(canvas, offset);
		}
		else{
		drawMarker(canvas, offset);									// Draw Position Marker with a specified Offset
		}

	}

	/**
	 * This method for a Given canvas gets the tiles from the visible region and then
	 * draws them on the View with an specific offset
	 * @param canvas
	 * @param offset
	 */
	void drawTiles(Canvas canvas, Point offset){
		synchronized (tileProvider.tilesLock){
			int tmpZoom = tileManager.getZoom();
			int maxIndex = (int) Math.pow(2, tmpZoom) - 1;
			for (int x = visibleRegion.left; x <= visibleRegion.right; x++){
				if (x < 0 || x > maxIndex){ 
					continue;
				}
				for (int y = visibleRegion.top; y <= visibleRegion.bottom; y++){
					if (y < 0 || y > maxIndex) {
						continue;
					}
					long tileX = x * TilesManager.TILE_SIZE;
					long tileY = y* TilesManager.TILE_SIZE;
					long finalX = tileX - offset.x;
					long finalY = tileY - offset.y;
					//Draw the Bitmap of the Tiles if the Tile is in the Cache
					if (tileProvider.getTileFromMemCache(x + ":" + y + ":" + tmpZoom) != null){
						canvas.drawBitmap(tileProvider.getTileFromMemCache(x + ":" + y+ ":" + tmpZoom).getImg(), finalX, finalY, bitmapPaint);
					}
				}
			}		
		}
	}
	/**
	 * This method draws the Marked on the current Marker location
	 * @param canvas
	 * @param offset
	 */
	void drawMarker(Canvas canvas, Point offset){

		if (MarkerLocation != null){
			Point markerPos = tileManager.lonLatToPixelXY(MarkerLocation.getLongitude(), MarkerLocation.getLatitude());
			int markerX = markerPos.x - offset.x;
			int markerY = markerPos.y - offset.y;
			//Draw Position Marker
			canvas.drawBitmap(  positionMarker, 
					markerX - positionMarker.getWidth() / 2,
					markerY - positionMarker.getHeight() / 2,
					bitmapPaint);
			//Calculate the Radio in Pixels of our Accuracy Circle
			float rad = MarkerLocation.getAccuracy() / (float)tileManager.calcMetersPerPixel(MarkerLocation.getLatitude());
			canvas.drawCircle(markerX, markerY, rad, circlePaint);

			int lineHeigth = 50;
			canvas.drawText("lon:" + MarkerLocation.getLongitude() + " lat:" + MarkerLocation.getLatitude() + " Zoom:" + tileManager.getZoom() 
							, 50, 80, fontPaint);
			canvas.drawText("HitCount: " + tileProvider.getCacheHitRate(), 50, 80 + lineHeigth, fontPaint);
			canvas.drawText("MissCount: " + tileProvider.getCacheMissRate(), 50, 80 + lineHeigth*2, fontPaint);
			
			if(checkSides==true){
				canvas.drawText("left: " + tileManager.getVisibleRegionCoor().getLeft(), 50, 80 + lineHeigth*3, fontPaint);
				canvas.drawText("right: " + tileManager.getVisibleRegionCoor().getRight(), 50, 80 + lineHeigth*4, fontPaint);
				canvas.drawText("top: " + tileManager.getVisibleRegionCoor().getTop(), 50, 80 + lineHeigth*5, fontPaint);
				canvas.drawText("bottom: " + tileManager.getVisibleRegionCoor().getBottom(), 50, 80 + lineHeigth*6, fontPaint);
			}
			
		}
	}
	
	/**
	 * This method draws the compass on the current location
	 * @param canvas
	 * @param offset
	 */
	void drawCompass(Canvas canvas, Point offset){

        if (MarkerLocation != null){
	        Path pfad = new Path();
	        int canWidth = canvas.getWidth();
	        int canHeight = canvas.getHeight();
	        int length = Math.min(canWidth, canHeight);
			
			Point markerPos = tileManager.lonLatToPixelXY(MarkerLocation.getLongitude(), MarkerLocation.getLatitude());
			int markerX = markerPos.x - offset.x;
			int markerY = markerPos.y - offset.y;

			//Calculate the Radio in Pixels of our Accuracy Circle
			float rad = MarkerLocation.getAccuracy() / (float)tileManager.calcMetersPerPixel(MarkerLocation.getLatitude());
			canvas.drawCircle(markerX, markerY, rad, circlePaint);
	        pfad.moveTo(0,-length/20);

	        pfad.lineTo(length/28, length/24);
	        pfad.lineTo(-length/28, length/24);
	        pfad.close();
	        canvas.translate(markerX,markerY);
			canvas.rotate(angleCompass);
			canvas.drawPath(pfad, compassColor);
		}


	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		Log.i(TAG,"event" + event.getAction());
		float eventX = event.getX();
		float eventY = event.getY();
		mScaleDetector.onTouchEvent(event);
		this.mDetector.onTouchEvent(event);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:		//When Finger touches screen
			lastTouchPos.setX(eventX);
			lastTouchPos.setY(eventY);
			return true;
		case MotionEvent.ACTION_MOVE:		//When Moving finger on the screen
			autoFollow = false;
			PointDouble current = new PointDouble(eventX, eventY);
			PointDouble deltaMovement = new PointDouble(current.getX()- lastTouchPos.getX(),
					current.getY() - lastTouchPos.getY());
			Point pixels1 = tileManager.lonLatToPixelXY(CenterViewLocation.getX(), CenterViewLocation.getY());
			Point pixels2 = new Point(pixels1.x - (int) deltaMovement.getX(), pixels1.y - (int) deltaMovement.getY());
			PointDouble newViewCenterLocation = tileManager.pixelXYToLonLat((int) pixels2.x, (int) pixels2.y);
			Log.i(TAG,"event" + newViewCenterLocation.toString());
			CenterViewLocation = newViewCenterLocation;			//Update Center of the View
			// Refresh the view
			refresh();
			// Prepare next drag
			lastTouchPos.setX(current.getX());
			lastTouchPos.setY(current.getY());
			return true;
		case MotionEvent.ACTION_UP:					//When Finger is released from the screen
			// nothing to do
			break;
		default:
			return false;
		}
		return super.onTouchEvent(event);
	}

	/**
	 * Refresh the View
	 */
	public void refresh(){
		fetchTiles();
		calculateOffset();
		invalidate();
	}

	/**
	 * Refresh from a non UI thread
	 */
	public void postRefresh(){
		fetchTiles();
		calculateOffset();
		postInvalidate();
	}

	/**
	 * Turns on the follow me mode... the location is automatically updated as position updates
	 */
	public void turnOnDriveMode(){
		if (MarkerLocation != null){
			CenterViewLocation.setX(MarkerLocation.getLongitude());
			CenterViewLocation.setY(MarkerLocation.getLatitude());
			autoFollow = true;
			refresh();
		}
	}
	/**
	 * Gets the Zoom level from the TileManager
	 * @return
	 */
	public int getZoom(){
		return tileManager.getZoom();
	}
	/**
	 * Sets the Zoom Level on the TileManager and request the new tiles
	 * @param zoom
	 */
	public void setZoom(int zoom){
		tileManager.setZoom(zoom);
		refresh();
	}
	/**
	 * Do a ZoomIn in TileManager and refresh the View
	 */
	public void zoomIn(){
		tileManager.zoomIn();
		refresh();
	}

	/**
	 * Do a ZoomOut in TileManager and refresh the View
	 */
	public void zoomOut(){
		tileManager.zoomOut();
		refresh();
	}

	/**
	 * Returns the current GPS location
	 * @return
	 */
	public Location getGpsLocation(){
		return MarkerLocation;
	}

	/**
	 * Returns the coordinates of our View Center
	 * @return
	 */
	public PointDouble getSeekLocation(){
		return CenterViewLocation;
	}

	/**
	 * Centers the given GPS coordinates in our view
	 * @param longitude
	 * @param latitude
	 */
	public void setCurrentLocation(double longitude, double latitude){
		CenterViewLocation.setX(longitude);
		CenterViewLocation.setY(latitude); 
	}

	/**
	 * Sets the marker location to a given Location
	 * @param location
	 */
	public void setMarkerLocation(Location location){
		MarkerLocation = location;
	}
	/**
	 * Sets the marker location to a manually given location
	 * @param longitude
	 * @param latitude
	 * @param altitude
	 * @param accuracy
	 */
	public void setMarkerLocation(double longitude, double latitude, double altitude, float accuracy){
		if (MarkerLocation == null) {
			MarkerLocation = new Location("");
		}
		MarkerLocation.setLongitude(longitude);
		MarkerLocation.setLatitude(latitude);
		MarkerLocation.setAltitude(altitude);
		MarkerLocation.setAccuracy(accuracy);
		if (autoFollow) {
			
			turnOnDriveMode();
			};

	}
	
	/**
	 * Set up the compass color
	 */
	private  void setupCompass(){
		compassColor.setAntiAlias(true);
        compassColor.setColor(Color.BLUE);
        compassColor.setStyle(Paint.Style.FILL);
	}
	
	/**
	 * Sets the angle of the compass used on follow me mode
	 * @param angle
	 */
    public void setAngleCompass(float angle) {
		this.angleCompass = angle;
		invalidate();
	}
    
    
   public double get_Left(){
	   return tileManager.getVisibleRegionCoor().getLeft();
   }
   public double get_Right(){
	   return tileManager.getVisibleRegionCoor().getRight();
   }
   public double get_Top(){
	   return tileManager.getVisibleRegionCoor().getTop();
   }
   public double get_Bottom(){
	   return tileManager.getVisibleRegionCoor().getBottom();
   }


	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		/**
		 * Detector for scaling
		 */
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();
			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 18.0f));
			tileProvider.cancelDownloads();
			setZoom((int) mScaleFactor);
			return true;
		}
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		private static final String DEBUG_TAG = "Gestures";       
		/**
		 * Detector when double touch is used..
		 */
		@Override
		public boolean onDoubleTap(MotionEvent event) {
			Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
			PointDouble current = new PointDouble(event.getX(), event.getY());
			PointDouble deltaMovement = new PointDouble(current.getX()- lastTouchPos.getX(),
														current.getY() - lastTouchPos.getY());
			Point pixels1 = tileManager.lonLatToPixelXY(CenterViewLocation.getX(), CenterViewLocation.getY());
			Point pixels2 = new Point(pixels1.x - (int) deltaMovement.getX(), pixels1.y - (int) deltaMovement.getY());
			PointDouble newViewCenterLocation = tileManager.pixelXYToLonLat((int) pixels2.x, (int) pixels2.y);
			Log.i(TAG,"event" + newViewCenterLocation.toString());
			CenterViewLocation = newViewCenterLocation;			//Update Center of the View
			
			zoomIn();
			return true;
		}
	}

}