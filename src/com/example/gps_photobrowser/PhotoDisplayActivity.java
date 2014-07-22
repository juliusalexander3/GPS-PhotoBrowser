package com.example.gps_photobrowser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.download.common.DownloadPictureThread;
import com.download.common.PhotoObject;
import com.download.common.PictureThreadPool;
import com.download.common.searchResponseObject;
import com.flickr.DOMparserXMLRPC;
import com.flickr.HandlerSearchResponseForSAX;
import com.flickr.GetInfoThread;
import com.flickr.SearchTaskFlickr;
import com.panoramio.SearchTaskPanoramio;
import com.threads.interfaces.HandleGetInfoFlickrCallback;
import com.threads.interfaces.HandlePictureDownloadedCallback;
import com.threads.interfaces.HandleSearchResultFlickrCallback;
import com.threads.interfaces.HandleSearchResultPanoramioCallback;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class PhotoDisplayActivity extends Activity implements  HandlePictureDownloadedCallback, HandleSearchResultFlickrCallback, HandleSearchResultPanoramioCallback, HandleGetInfoFlickrCallback{

	private static final String TAG = "PhotoDisplayActivity";
	Button nextButton, reverseButton;
	TextView IDphotoView, TitlePhotoView;
	ImageSwitcher mySwitcher;
	SearchTaskFlickr mSearchTask;
	SearchTaskPanoramio mSearchTaskPano;
	private byte[] XMLresponse = null;	
	private byte[] JSONresponse = null;	
	Bitmap bmp1, bmp2;
	int currentIndex=-1;
	int maxSizeCounter =0;
	boolean XMLread =false;
	boolean JSONread=false;
	boolean isFlickr = false;
	boolean isPanoramio =false;

	//******************************
	ArrayList<searchResponseObject> picturesInXML = new ArrayList<searchResponseObject>();
	ArrayList<PhotoObject> downloadedPhotoObjects = new ArrayList<PhotoObject>();
	PictureThreadPool mPictThreadPool;
	JSONObject pictureInJSON;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo_display);
		//Retrieve Intent information, coordinates send by map activity and if is requesting pictures from Flickr or Panoramio
		Intent intent = getIntent();
		if(intent.getStringExtra("Left") != null){
			if(intent.getStringExtra("From").equals("flickr")){
				isFlickr=true;
				isPanoramio=false;
			}else if(intent.getStringExtra("From").equals("panoramio")){
				isFlickr=false;
				isPanoramio=true;
			}
			double LeftCoor = Double.parseDouble(intent.getStringExtra("Left"));
			double BottomCoor = Double.parseDouble(intent.getStringExtra("Bottom"));
			double RightCoor = Double.parseDouble(intent.getStringExtra("Right"));
			double TopCoor = Double.parseDouble(intent.getStringExtra("Top"));
			//*******************Request list from Flickr with REST, and download Pictures from Flickr XML response************************************
			if(isFlickr==true){
				mSearchTask = new SearchTaskFlickr(this, LeftCoor, BottomCoor, RightCoor, TopCoor);
				mSearchTask.start();
			}
			//*******************Request list from Panoramio with REST, and download Pictures from Panoramio************************************		
			if(isPanoramio==true){
				mSearchTaskPano = new SearchTaskPanoramio(this, LeftCoor, BottomCoor, RightCoor, TopCoor);
				mSearchTaskPano.start();		
			}

			//****************************************************************************************************			
		}
		// Get The references to the buttons and views and assign them
		nextButton = (Button) findViewById(R.id.button1);
		reverseButton = (Button) findViewById(R.id.button2);
		IDphotoView = (TextView) findViewById(R.id.textView1);
		TitlePhotoView = (TextView) findViewById(R.id.textView2);
		mySwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher1);
		
		mySwitcher.setFactory(new ViewSwitcher.ViewFactory() {
			public View makeView() {
				// Create a new ImageView set it's properties 
				ImageView imageView = new ImageView(getApplicationContext());
				imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
				imageView.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
				return imageView;
			}
		});

		// Declare the animations and initialize them
		Animation in = AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left);
		Animation out = AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right);

		// set the animation type to imageSwitcher
		mySwitcher.setInAnimation(in);
		mySwitcher.setOutAnimation(out);
		nextButton.setOnClickListener(NextListener);
		reverseButton.setOnClickListener(ReverseListener);

	}

	/**
 	* Click listener for next button
 	*/
	private OnClickListener NextListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			maxSizeCounter = downloadedPhotoObjects.size();
			currentIndex++; 
			if (currentIndex>maxSizeCounter-1){
				currentIndex =0;
			}
			if (currentIndex >=0 && currentIndex <=maxSizeCounter-1){

				try {
					setDataInfo(currentIndex);
				} catch (InterruptedException e) {
					Log.e(TAG,"Error NextButton: " + e.toString() );
				}
			}
		}

	};

	/**
	 * Click listener for back button
	 */
	private OnClickListener ReverseListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			maxSizeCounter = downloadedPhotoObjects.size();
			currentIndex--; 
			if (currentIndex<0){
				currentIndex = maxSizeCounter-1;
			}
			if (currentIndex >=0 && currentIndex <=maxSizeCounter-1){

				try {
					setDataInfo(currentIndex);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Log.e(TAG,"Error BackButton: " + e.toString() );
				}
			}
		}

	};


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photo_display, menu);
		return true;
	}

	/**
	 * Parse search XML as byteArray and returns an list of searchResponseObjects
	 * @return
	 */
	public ArrayList<searchResponseObject> parseSearchXMLfileFlickr(){
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(false);
		parserFactory.setValidating(false);
		SAXParser parser = null;
		try {
			parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			Log.e(TAG,"ParserConfigurationException " + e);
		} catch (SAXException e) {
			Log.e(TAG,"SAXException " + e);
		}

		HandlerSearchResponseForSAX handler = new HandlerSearchResponseForSAX();
		InputStream is = new ByteArrayInputStream(this.XMLresponse);
		try {
			parser.parse(is, handler);
		} catch (SAXException e) {
			Log.e(TAG,"SAXException " + e);
		} catch (IOException e) {
			Log.e(TAG,"IOException " + e);
		}

		Log.i(TAG,"Finished Parse");
		return handler.getMyPhotos();
	}

	/**
	 * parse JSONresponse from Panoramio
	 */
	public void parseJSONresponse(){
		JSONObject obj =null;
		try {
			obj = new JSONObject(new String(JSONresponse));
			JSONArray photos = obj.getJSONArray("photos");
			for(int i=0; i<photos.length();i++){
				picturesInXML.add(
						new searchResponseObject(
								photos.getJSONObject(i).getString("photo_id"),
								photos.getJSONObject(i).getString("owner_name"),
								photos.getJSONObject(i).getString("photo_title"))
						);
			}
		} catch (JSONException e) {
			Log.e(TAG,"IOException " + e);
		}
	}


	/**
	 * Add the pictures to the list of Photo objects
	 */
	@Override
	public synchronized void handlePictureDownload(DownloadPictureThread myThread) {
		if(isFlickr ==true){
			PhotoObject tmpPhotoObject= new PhotoObject();
			tmpPhotoObject.setId(myThread.getmPhotoInfo().getId());
			tmpPhotoObject.setOwner(myThread.getmPhotoInfo().getOwner());
			tmpPhotoObject.setSecret(myThread.getmPhotoInfo().getSecret());
			tmpPhotoObject.setServer(myThread.getmPhotoInfo().getServer());	
			tmpPhotoObject.setFarm(myThread.getmPhotoInfo().getFarm());
			tmpPhotoObject.setTitle(myThread.getmPhotoInfo().getTitle());
			tmpPhotoObject.setIspublic(myThread.getmPhotoInfo().getIspublic());
			tmpPhotoObject.setIsfriend(myThread.getmPhotoInfo().getIsfriend());
			tmpPhotoObject.setIsfamily(myThread.getmPhotoInfo().getIsfamily());
			tmpPhotoObject.setPicture(myThread.getBitmap());
			downloadedPhotoObjects.add(tmpPhotoObject);
		} else if (isPanoramio == true){
			PhotoObject tmpPhotoObject= new PhotoObject();
			tmpPhotoObject.setId(myThread.getmPhotoInfo().getId());
			tmpPhotoObject.setOwner(myThread.getmPhotoInfo().getOwner());
			tmpPhotoObject.setTitle(myThread.getmPhotoInfo().getTitle());
			tmpPhotoObject.setPicture(myThread.getBitmap());
			downloadedPhotoObjects.add(tmpPhotoObject);
		}
		if(downloadedPhotoObjects.size()==1){
			myThread.getHandler().obtainMessage(2).sendToTarget();
		}

	}

	/**
	 * Handle back button to destroy the activity
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Log.i(TAG,"Back Button Pressed");
			Log.i(TAG,"Downloads Cancelled");
			try{
				Log.i(TAG,"finish");
				finish();
			}catch (Exception e){
				Log.e(TAG,"Error: " + e.toString() );
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	/**
	 * Set Information on the TextViews
	 * @param index
	 * @throws InterruptedException
	 */
	public void setDataInfo(int index) throws InterruptedException{
		if(downloadedPhotoObjects.get(index).isInformationFetched()==false  && isFlickr==true){
			GetInfoThread myInfoTask= new GetInfoThread(downloadedPhotoObjects.get(index).getId(),this, index, mHandler);
			myInfoTask.start();			
		} else if(downloadedPhotoObjects.get(index).isInformationFetched()==true  && isFlickr==true){

			Toast toast = Toast.makeText(getApplicationContext(), 
					" Title: "    + downloadedPhotoObjects.get(index).getId()+ "\n"+
							" Realname: " + downloadedPhotoObjects.get(index).getRealname() +  "\n"+
							" Username: " + downloadedPhotoObjects.get(index).getUser() +  "\n" +
							" ID: " + downloadedPhotoObjects.get(index).getId()
							, Toast.LENGTH_SHORT);
			toast.show();	
		}

		IDphotoView.setText("ID: " + downloadedPhotoObjects.get(index).getId() + " user: " +downloadedPhotoObjects.get(index).getUser() );
		TitlePhotoView.setText("Title: " + downloadedPhotoObjects.get(index).getTitle() +  " realname: " + downloadedPhotoObjects.get(index).getRealname());
		BitmapDrawable bitmapDrawable = new BitmapDrawable(getApplicationContext().getResources(),downloadedPhotoObjects.get(index).getPicture());
		mySwitcher.setImageDrawable(bitmapDrawable);

	}
	
	
	/**
	 * Handle Search Result from Flickr and start Thread Pool to download pictures.
	 */
	@Override
	public void handleSearchResultFlickr(SearchTaskFlickr mySearchTask) {
		int state = mySearchTask.getThreadState();
		if (state == mySearchTask.TASK_FINISHED){
			this.XMLresponse = mySearchTask.getXMLresponse();
			this.XMLread = true;
			Log.i(TAG,"Search Result Arrived");
			try {
				Log.i(TAG,"Joined thread");
				if (XMLread == true){
					Log.i(TAG,"Before parse");
					picturesInXML = parseSearchXMLfileFlickr();
					Log.i(TAG,"XML Loaded");
					mPictThreadPool = new PictureThreadPool(15, this, mHandler2);
					for (int i=0; i< picturesInXML.size();i++){
						mPictThreadPool.downloadImage("m", picturesInXML.get(i));
					}
					maxSizeCounter = picturesInXML.size();
				}
			} catch (Exception e) {
				Log.e(TAG,"Error when downloading Flickr " +  e);
			}

		} else if(state == mySearchTask.TASK_FAILED){
			Log.i(TAG,"Task Failed");
		}
	}

	/**
	 * Handle Search Result from Panoramio and start Thread Pool to download pictures.
	 */
	@Override
	public void handleSearchResultPan(SearchTaskPanoramio mySearchTask) {
		int state = mySearchTask.getThreadState();
		if (state == mySearchTask.TASK_FINISHED){
			this.JSONresponse = mySearchTask.getJSONresponse();
			this.JSONread = true;
			Log.i(TAG,"Search Result Arrived");
			try {
				Log.i(TAG,"Joined thread");
				if (JSONread == true){
					parseJSONresponse();
					Log.i(TAG,"JSON Loaded");

					mPictThreadPool = new PictureThreadPool(15, this, mHandler2);
					for (int i=0; i< picturesInXML.size();i++){
						mPictThreadPool.downloadImagePan(picturesInXML.get(i));
					}
					maxSizeCounter = picturesInXML.size();
				}
			} catch (Exception e) {
				Log.e(TAG,"Error when downloading Panoramio " +  e);
			}
		} else if(state == mySearchTask.TASK_FAILED){
			Log.i(TAG,"Task Failed");
		}
	}

	/**
	 * Handles the Result from the getInfo XML-RPC response
	 */
	@Override
	public void handleInfoResult(GetInfoThread myGetInfoTask) {
		if(myGetInfoTask.getThreadState() == GetInfoThread.TASK_FINISHED){
			Log.i(TAG,"Handle Result");
			DOMparserXMLRPC tempDom = new DOMparserXMLRPC(myGetInfoTask.getXMLresponse());
			downloadedPhotoObjects.get(myGetInfoTask.getIndex()).setRealname(tempDom.getmRealname());
			downloadedPhotoObjects.get(myGetInfoTask.getIndex()).setUser(tempDom.getmUsername());
			downloadedPhotoObjects.get(myGetInfoTask.getIndex()).setInformationFetched(true);
			myGetInfoTask.getmHandler().obtainMessage(myGetInfoTask.getIndex()).sendToTarget();
		}

	}
	/**
	 * Handler to display toast element
	 */
	private  final  Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			Log.i(TAG,"msg" + msg.what);
			Toast toast = Toast.makeText(getApplicationContext(), 
					" Title: "    + downloadedPhotoObjects.get(msg.what).getTitle()+ "\n"+
							" Realname: " + downloadedPhotoObjects.get(msg.what).getRealname() +  "\n"+
							" Username: " + downloadedPhotoObjects.get(msg.what).getUser() +  "\n" +
							" ID: " + downloadedPhotoObjects.get(msg.what).getId()
							, Toast.LENGTH_SHORT);
			toast.show();	
		}
	};

	/**
	 * Handler to display first picture
	 */
	private  final  Handler mHandler2 = new Handler(){
		@Override
		public void handleMessage(Message msg){
			Log.i(TAG,"msg" + msg.what);
			if (msg.what == 2){
				BitmapDrawable bitmapDrawable = new BitmapDrawable(getApplicationContext().getResources(),downloadedPhotoObjects.get(0).getPicture());
				mySwitcher.setImageDrawable(bitmapDrawable);
			}
		}
	};



}
