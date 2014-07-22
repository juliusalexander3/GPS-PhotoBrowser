package com.flickr;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.download.common.searchResponseObject;

public class HandlerSearchResponseForSAX extends DefaultHandler {
	ArrayList<searchResponseObject> myPhotos = new ArrayList<searchResponseObject>();
	@Override
	public void startElement(String uri, String localName, String qName, Attributes a) {

		if (qName.equals("photo")){
			searchResponseObject tmp =new searchResponseObject();
			tmp.setId(a.getValue("id"));
			tmp.setOwner(a.getValue("owner"));
			tmp.setSecret(a.getValue("secret"));
			tmp.setServer(a.getValue("server"));
			tmp.setFarm(a.getValue("farm"));
			tmp.setTitle(a.getValue("title"));
			tmp.setIspublic(a.getValue("ispublic"));
			tmp.setIsfriend(a.getValue("isfriend"));
			tmp.setIsfamily(a.getValue("isfamily"));			
			myPhotos.add(tmp);
		}
	}
	public void endDocument() {
	}
	public ArrayList<searchResponseObject> getMyPhotos() {
		return myPhotos;
	}

	
}