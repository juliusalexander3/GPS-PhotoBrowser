package com.download.common;

public class searchResponseObject {
	private String id;
	private String owner;
	private String secret;
	private String server;
	private String farm;
	private String title;
	private boolean ispublic;
	private boolean isfriend;
	private boolean isfamily;
	private String urlPanoramio = "";

	public searchResponseObject(){

	}
	public searchResponseObject(String id, String owner, String title){
		this.id=id;
		this.owner = owner;
		this.title = title;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getFarm() {
		return farm;
	}

	public void setFarm(String farm) {
		this.farm = farm;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean getIspublic() {
		return ispublic;
	}

	public void setIspublic(String ispublic) {
		if (ispublic.equals("1")){
			this.ispublic = true;
		}else if (ispublic.equals("0")){
			this.ispublic = false;
		}
	}

	public boolean getIsfriend() {
		return isfriend;
	}

	public void setIsfriend(String isfriend) {
		if (isfriend.equals("1")){
			this.isfriend = true;
		}else if (isfriend.equals("0")){
			this.isfriend = false;
		}
	}

	public boolean getIsfamily() {
		return isfamily;
	}

	public void setIsfamily(String isfamily) {
		if (isfamily.equals("1")){
			this.isfamily = true;
		}else if (isfamily.equals("0")){
			this.isfamily = false;
		}
	}

	public void setURLPanoramio(String url){
		this.urlPanoramio = url;
	}
	/**
	 * Size is specified with letters m,s,t,b,o
	 * s = small square 75 x 75
	 * t = thumbnail 100 on the longest side
	 * m = small 240 on the longest side
	 * - = medium 500 on the longest side
	 * b = large 1024 on the longest side
	 * o = original size
	 * @param size
	 * @return
	 */
	public String getURLtoDownload(String size){
		return "http://farm"+farm+".static.flickr.com/"+server+"/"+id+"_"+secret+"_"+size+".jpg";
	}
	public String getURLtoDownloadPan(){
		return "http://mw2.google.com/mw-panoramio/photos/small/" + id + ".jpg";
	}

}
