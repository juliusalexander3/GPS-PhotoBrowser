package com.example.gps_photobrowser;


import custom.mapview.MapView;
import custom.mapview.TilesProvider;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class MapViewActivity extends Activity implements LocationListener, SensorEventListener {
	private static final String TAG = "MainActivity";
	//minTime	minimum time interval between location updates, in milliseconds
	private long MIN_TIME_MS = 1000 * 1;//each second
	private long MIN_TIME_MS_NETWORK = 1000 *35;
	//minDistance	minimum distance between location updates, in meters
	private float MIN_DISTANCE_IN_MTS = 0; //each 1 meters
	private float MIN_DISTANCE_IN_MTS_NETWORK = 0;

	private MapView mapView;
	TilesProvider tilesProvider;
	Location savedGpsLocation;
	boolean stopped = false;
	protected LocationManager locationManager;
	private boolean isGPSEnabled  = false;
	private boolean isNetworkEnabled = false;
	boolean canGetLocation = false;
	Location location;
	private LinearLayout mLayout;
	private LinearLayout mLayout2;
	private Button myButton;
	private Button myButton2;
	private Button myButton3;
	
    private SensorManager sensorManager;
    private Sensor magnetfeldSensor;
    private Sensor accelerometerSensor;
    
    private float[] mGravs = new float[3];
    private float[] mGeoMags = new float[3];
    private float[] mOrientation = new float[3];
    private float[] mRotationM = new float[9]; 
    private float[] mRotationM2 = new float[9];
	
	private OnClickListener DriveListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			mapView.turnOnDriveMode();
		}
		
	};
	
	
	private OnClickListener FetchPhotosListenerFlickr = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Intent myIntent = new Intent(getApplicationContext(), PhotoDisplayActivity.class);
			myIntent.putExtra("Left", Double.toString(mapView.get_Left()));
			myIntent.putExtra("Bottom",  Double.toString(mapView.get_Bottom()));
			myIntent.putExtra("Top",  Double.toString(mapView.get_Top()));
			myIntent.putExtra("Right",  Double.toString(mapView.get_Right()));
			myIntent.putExtra("From", "flickr");
			startActivity(myIntent);
		}
		
	};
	
	private OnClickListener FetchPhotosListenerPano = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Intent myIntent = new Intent(getApplicationContext(), PhotoDisplayActivity.class);
			myIntent.putExtra("Left", Double.toString(mapView.get_Left()));
			myIntent.putExtra("Bottom",  Double.toString(mapView.get_Bottom()));
			myIntent.putExtra("Top",  Double.toString(mapView.get_Top()));
			myIntent.putExtra("Right",  Double.toString(mapView.get_Right()));
			myIntent.putExtra("From", "panoramio");
			startActivity(myIntent);
		}
		
	};


	 Handler newTileHandler = new Handler(){
		// This is executed on the UI thread
		public void handleMessage(android.os.Message msg){
			// Ask the mapView to redraw itself
			if (mapView != null) {
				mapView.invalidate();
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_view);
		initializeView();
		
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        magnetfeldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//
		myButton = new Button(this);
		myButton.setText("Drive Me");
		myButton.setOnClickListener(DriveListener);
		
		myButton2 = new Button(this);
		myButton2.setText("Flickr");
		myButton2.setOnClickListener(FetchPhotosListenerFlickr);
		//
		myButton3 = new Button(this);
		myButton3.setText("Panoramio");
		myButton3.setOnClickListener(FetchPhotosListenerPano);
		//
		mLayout = (LinearLayout) findViewById(R.id.layout1);
		mLayout2 = (LinearLayout) findViewById(R.id.layout2);
		mLayout.addView(mapView);
		mLayout2.addView(myButton);
		mLayout2.addView(myButton2);
		mLayout2.addView(myButton3);
	}

	@Override
	protected void onResume(){
		Log.i(TAG, "RequestLocation Update");
		getLocation();
	    sensorManager.registerListener(this, magnetfeldSensor, SensorManager.SENSOR_DELAY_GAME);
	    sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
		super.onStop();
	}


	@Override
	protected void onStop(){
		locationManager.removeUpdates(this);
	    sensorManager.unregisterListener(this);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map_view, menu);
		return true;
	}

	/**
	 * Do Initial SetUp of the View
	 */
	void initializeView(){
		Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.pos_marker);
		tilesProvider = new TilesProvider(newTileHandler);
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();
		int height = display.getHeight()-300;	//300 is actually a value for a 1280 by 720 screen, need to check
												//with other devices a correct ratio to not be used..
		mapView = new MapView(this, width , height , tilesProvider, marker);
		mapView.refresh();
	}

	@Override
	public void onLocationChanged(Location location) {
		if (!stopped && location != null){
			// Set location and update the mapView
			mapView.setMarkerLocation(location.getLongitude(), location.getLatitude(), location.getAltitude(), location.getAccuracy());
			mapView.postInvalidate();
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	public void getLocation() {
		try {
			// Acquire a reference to the system Location Manager
			locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);   
			// Check if GPS is enabled
			isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			Log.i(TAG, "GPS provider enabled: " + Boolean.toString(isGPSEnabled));
			// Check if NetworkPosition is enabled
			isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			Log.i(TAG, "Network provider enabled: " + Boolean.toString(isNetworkEnabled));

			if (!isGPSEnabled && !isNetworkEnabled) {
				// There is no way to get location, nothing else to do here
			} 
			else
			{
				//the location is possible so I set the flag to true
				this.canGetLocation = true;

				// First try to get location from Network Provider                  
				if (isNetworkEnabled) {                               	
					// Register the listener with the Location Manager to receive location updates                   	
					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_MS_NETWORK, MIN_DISTANCE_IN_MTS_NETWORK, this);
					Log.d(TAG, "reqUpdates using Network");
					//Only if the location retrieves position, this is read
					if (locationManager != null) {
						location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					}
				}

				// if GPS Enabled setup update with GPS and get lat/long using GPS Services
				if (isGPSEnabled) {     
					// Register the listener with the Location Manager to receive location updates 
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MS, MIN_DISTANCE_IN_MTS, this);
					Log.d(TAG, " reqUpdates using GPS");
					//Try to get the last Known location from the GPS
					if (locationManager != null) {
						if (isNetworkEnabled && isGPSEnabled){
							if(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getTime()>location.getTime());
							location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						}
					}                    
				}	

			}//finished the setup

		} catch (Exception e) {
			Log.e(TAG, "Exception in getLocation" + e.toString());
		}

	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()){
		case Sensor.TYPE_ACCELEROMETER:
			mGravs[0]=event.values[0];
			mGravs[1]=event.values[1];
			mGravs[2]=event.values[2];
			Log.d("Sensors","copiedAccelerometer");
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mGeoMags[0]=event.values[0];
			mGeoMags[1]=event.values[1];
			mGeoMags[2]=event.values[2];
			Log.d("Sensors","copiedMagnetic");
			break;
		default: 
			Log.d("Sensors","default");
			break;
		}

		if(SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags))	{
			Log.d("Sensors","rotation Matrix succesfull");
			SensorManager.remapCoordinateSystem(mRotationM, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationM2);
			SensorManager.getOrientation(mRotationM, mOrientation);
			Log.d("Sensors","Orientation" + Math.toDegrees(mOrientation[0]));
			if (mapView != null) {
				//view.setWinkel(-event.values[0]);
				mapView.setAngleCompass((float)Math.toDegrees(mOrientation[0]));
			}
		}

	}

}
