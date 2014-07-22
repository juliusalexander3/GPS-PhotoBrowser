package com.download.common;

import android.graphics.Bitmap;

public class PhotoObject {
	private String id="";
	private String owner="";
	private String secret;
	private String server;
	private String farm;
	private String title="";
	private boolean ispublic;
	private boolean isfriend;
	private boolean isfamily;
	private Bitmap picture;
	private String user="";
	private String realname="";
	private boolean InformationFetched = false;
	
	public PhotoObject(){
		
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

	public boolean isIspublic() {
		return ispublic;
	}

	public void setIspublic(boolean ispublic) {
		this.ispublic = ispublic;
	}

	public boolean isIsfriend() {
		return isfriend;
	}

	public void setIsfriend(boolean isfriend) {
		this.isfriend = isfriend;
	}

	public boolean isIsfamily() {
		return isfamily;
	}

	public void setIsfamily(boolean isfamily) {
		this.isfamily = isfamily;
	}

	public Bitmap getPicture() {
		return picture;
	}

	public void setPicture(Bitmap picture) {
		this.picture = picture;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public boolean isInformationFetched() {
		return InformationFetched;
	}

	public void setInformationFetched(boolean informationFetched) {
		InformationFetched = informationFetched;
	}
}
