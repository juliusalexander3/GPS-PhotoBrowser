package custom.mapview;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;


public class TilesManager
{
	//CONSTANTS
	private static final String TAG = "TilesManager";				//Tag for debugging
	public final static double EARTH_RADIUS = 6378137; 			  	// Earth Radio in meters
	public final static double MIN_EARTH_LATITUDE = -85.05112878; 	// Min Latitude South pole
	public final static double MAX_EARTH_LATITUDE = 85.05112878; 	// Max Latitude North pole
	public final static double MIN_EARTH_LONGITUDE = -180; 			// Min Longitude West
	public final static double MAX_EARTH_LONGITUDE = 180; 			// Min Latitude East
	public final static int    TILE_SIZE = 256;						// Size in pixels of a single tile image
	protected final static int MAX_ZOOM = 18;						//Max Zoom Value of OpenStreetMaps
	protected final static int MIN_ZOOM = 0;						//Min Zoom Value of OpenStreetMaps

	//Variables
	protected int viewWidth, viewHeight;							//Pixels of the View where Map is rendered
	protected int NumberTilesInX, NumberTilesInY;					//Number of Tiles used for the whole View
	protected Rect visibleRegion;									//Hold Index of Visible tiles
	protected RectDouble visibleRegionCoord;
	protected PointDouble location = new PointDouble(0, 0);			//Current Locations TileManager inital 0,0
	protected int zoom = 3;											//Default zoom level 3

	/**
	 * Creates a tile Manager that is going to perform calculations for the required tiles
	 * this calculations are going to depend on the View Size, when created the visible
	 * region is set to coordinates 0,0 and zoom 3
	 * @param viewWidth  is the Width in pixels of the View to be used
	 * @param viewHeight  is the Height in pixels of the View to be used
	 */
	public TilesManager(int viewWidth, int viewHeight){
		this.viewWidth = viewWidth;
		this.viewHeight = viewHeight;
		NumberTilesInX  = (int) ((float) viewWidth / TILE_SIZE);
		NumberTilesInY = (int) ((float) viewHeight / TILE_SIZE);
		updateVisibleRegion(location.getX(), location.getY(), zoom);	//When created default position 0,0 zoom level 3
	}

	/**
	 * Calculate the ratio used to calculate the x and y tiles
	 * ratio is a value that multiplied by 2^zoom the tile number can be found
	 * @param longitude 
	 * @param latitude 
	 * @return Calculated Ratio
	 */
	public static PointDouble calcRatio(double longitude, double latitude){
		double ratioX = ((longitude + 180.0) / 360.0);
		double ratioY = ( (1 - Math.log(Math.tan(Math.toRadians(latitude)) + 1 / Math.cos(Math.toRadians(latitude))) / Math.PI) / 2);
		return new PointDouble(ratioX, ratioY);
	}
	/**
	 * Get the number of Tiles that represent the map
	 * @return int number of tiles of the map either on x or y
	 */
	public int getMapSizeInTiles(){
		return (int) Math.pow(2, zoom);
	}

	/**
	 * Calculate the Index of a tile for a specified longitude and latitude
	 * For the current zoom level on the TileManager
	 * @param longitude 
	 * @param latitude 
	 * @return Point Tile Number in x and y directions
	 */
	protected Point calcTileIndex(double longitude, double latitude){
		Point tmpTileIndex;
		PointDouble ratio = calcRatio(longitude, latitude);
		int mapSize = getMapSizeInTiles();		
		tmpTileIndex = new Point((int) (ratio.getX() * mapSize), (int) (ratio.getY() * mapSize));
		return tmpTileIndex;
	}

	/**
	 * Updates the region that is Visible, set the points of visibleRegion as a Rect
	 * For the given longitude, latitude and zoom level calculates the required tile as
	 * well as neighboring Tiles
	 * @param  longitude
	 * @param  latitude
	 * @param  zoom
	 */
	protected void updateVisibleRegion(double longitude, double latitude, int zoom){
		// Update values from TileManager
		location.setX(longitude);
		location.setY(latitude);
		this.zoom = zoom;
		// Get the Index of the tile with the latitude and longitude we are requesting
		Point tileIndex = calcTileIndex(location.getX(), location.getY());
		// Get  Neighbor Tiles for Left, Right, Up and Down
		int halfNumberTilesInX = (int) ((float) (NumberTilesInX + 1) / 2f);
		int halfNumberTilesInY = (int) ((float) (NumberTilesInY + 1) / 2f);
		Log.i(TAG,"UVR tileIndex.x " + tileIndex.x + " tileIndex.y " + tileIndex.y);
		Log.i(TAG,"UVR NumberTilesInX " + NumberTilesInX + "NumberTilesInY " + NumberTilesInY);
		Log.i(TAG,"UVR Rect Left " + (tileIndex.x - NumberTilesInX) + " Top " + (tileIndex.y - NumberTilesInY)
									+ " Rigth " + (tileIndex.x + NumberTilesInX) + "Bottom " + (tileIndex.y + NumberTilesInY));
		//Set the left, top, right, bottom values for the visible Region
		visibleRegion = new Rect(tileIndex.x - NumberTilesInX,
								 tileIndex.y - NumberTilesInY,
								 tileIndex.x + NumberTilesInX,
								 tileIndex.y + NumberTilesInY);
		updateCoorVisibleRegion();
		
	}

	/**
	 * Check if the x values is on the range of min and max and returns it
	 * in case x is below min bound, returns min in case is above max bound returns max
	 * if its on the range returns x.
	 * @param x Value to be checked
	 * @param min Lower Bound
	 * @param max Upper Bound
	 * @return
	 */
	protected  double getValueInRange(double x, double min, double max){
		return Math.min(Math.max(x, min), max);
	}
	/**
	 * Calculates for a given latitude, how many meter/pixel we have in our tiles
	 * @param latitude
	 * @return
	 */
	public double calcMetersPerPixel(double latitude){
		latitude = getValueInRange(latitude, MIN_EARTH_LATITUDE, MAX_EARTH_LATITUDE);
		return Math.cos(latitude * Math.PI / 180.0) * 2.0 * Math.PI * EARTH_RADIUS / (double) (TILE_SIZE * getMapSizeInTiles());
	}
	/**
	 * Calculates for a given longitude and latitude the pixel number representation
	 * according to the current State of the TileManager.
	 * @param  longitude 
	 * @param  latitude
	 * @return Point as a pixel in x and y for the current TileManager Status
	 */
	public Point lonLatToPixelXY(double longitude, double latitude){
		// Check if longitude and latitude are in range
		longitude = getValueInRange(longitude, MIN_EARTH_LONGITUDE, MAX_EARTH_LONGITUDE);
		latitude = getValueInRange(latitude, MIN_EARTH_LATITUDE, MAX_EARTH_LATITUDE);
		// Calculate the Ratio and store it
		PointDouble ratio = calcRatio(longitude, latitude);
		long mapSizeInPixels = getMapSizeInTiles() * TILE_SIZE;			//the number of total pixels in one direction for the map

		//Get the pixel number on the pixel range for the specified Latitude and Longitude
		int pixelX = (int) getValueInRange(ratio.getX() * mapSizeInPixels + 0.5, 0, mapSizeInPixels - 1);
		int pixelY = (int) getValueInRange(ratio.getY() * mapSizeInPixels + 0.5, 0, mapSizeInPixels - 1);
		return new Point(pixelX, pixelY);
	}
	/**
	 * Calculates for a given x,y pixel reference the latitude and longitude
	 * according to the current State of the TileManager.
	 * @param pixelX
	 * @param pixelY
	 * @return
	 */
	public PointDouble pixelXYToLonLat(int pixelX, int pixelY){
		double mapSizeInPixels = getMapSizeInTiles() * TILE_SIZE;
		double x = (getValueInRange(pixelX, 0, mapSizeInPixels - 1) / mapSizeInPixels) - 0.5;
		double y = 0.5 - (getValueInRange(pixelY, 0, mapSizeInPixels - 1) / mapSizeInPixels);
		double latitude = 90.0 - 360.0 * Math.atan(Math.exp(-y * 2.0 * Math.PI)) / Math.PI;
		double longitude = 360.0 * x;
		return new PointDouble(longitude, latitude);
	}

	/**
	 * Sets the location of the Tile Manager for a given longitude and latitude, updates the
	 * visible region according to the current zoom level
	 * @param longitude
	 * @param latitude
	 */
	public void setLocation(double longitude, double latitude){
		updateVisibleRegion(longitude, latitude, zoom);
	}
	/**
	 * Returns the VisibleRegion Coordinates
	 * @return Rect Visible Region
	 */
	public Rect getVisibleRegion(){
		return visibleRegion;
	}
	
	
	/**
	 * Returns the VisibleRegion Coordinates
	 * @return Rect Visible Region
	 */
	public RectDouble getVisibleRegionCoor(){
		return visibleRegionCoord;
	}

	public int getZoom(){
		return zoom;
	}
	/**
	 * Sets zoom to an specific level
	 * @param zoom
	 */
	public void setZoom(int zoom){
		zoom = (int) getValueInRange(zoom, MIN_ZOOM, MAX_ZOOM);
		updateVisibleRegion(location.getX(), location.getY(), zoom);
	}
	/**
	 * One level zoomIn
	 * @return
	 */
	public int zoomIn(){
		if (zoom < MAX_ZOOM){
			setZoom(zoom + 1);
		}
		return zoom;
	}
	/**
	 * One Level ZoomOut
	 * @return
	 */
	public int zoomOut(){
		if (zoom >MIN_ZOOM){
			setZoom(zoom - 1);
		}
		return zoom;
	}
	
	

	public void updateCoorVisibleRegion(){
		Point centerInPx = lonLatToPixelXY(location.getX(), location.getY());
		double mapSizeInPixels = getMapSizeInTiles() * TILE_SIZE;

		int leftInPx = (int) getValueInRange(centerInPx.x -(viewWidth/2),0,mapSizeInPixels);
		int rightInPx = (int) getValueInRange(centerInPx.x +(viewWidth/2),0,mapSizeInPixels);
		int topInPx = (int) getValueInRange(centerInPx.y -(viewHeight/2),0,mapSizeInPixels);
		int bottomInPx = (int) getValueInRange(centerInPx.y +(viewHeight/2),0,mapSizeInPixels);
		//Log.e(TAG,"left" + leftInPx + "top" + topInPx +"right" + rightInPx + "bottom" + bottomInPx);
	
		Point LeftTop = new Point(leftInPx,topInPx),
		  RightBottom = new Point(rightInPx,bottomInPx);
		//Log.e(TAG,"left" + LeftTop.x + "top" + LeftTop.y +"right" + RightBottom.x + "bottom" + RightBottom.y);
		
		PointDouble Lt, Rb;
		Lt = pixelXYToLonLat(LeftTop.x, LeftTop.y);
		Rb = pixelXYToLonLat(RightBottom.x, RightBottom.y);
		double leftCoor= Lt.getX();
		double rightCoor = Rb.getX();
		double topCoor = Lt.getY();
		double bottomCoor = Rb.getY();
		Log.e(TAG,"left" + leftCoor + "top" + topCoor +"right" + rightCoor + "bottom" + bottomCoor);
		RectDouble CoorVisR = new RectDouble(leftCoor, topCoor, rightCoor, bottomCoor);
		visibleRegionCoord = CoorVisR;
		
	}

}

