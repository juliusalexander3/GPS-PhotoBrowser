package com.flickr;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class DOMparserXMLRPC {

	private static final String TAG = "DOM_XML_parser_RPC";
	private String mId,mUsername,mRealname,mTitle;
/**
 * Create a DOMparserXMLRPC object, once created the object is parsed and the data is obtained
 * @param byteToParse is the XML response to getInfo Request
 */
	public DOMparserXMLRPC(byte[] byteToParse){


		try {
			DocumentBuilderFactory parserFactory = DocumentBuilderFactory.newInstance();
			parserFactory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder dBuilder = parserFactory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(byteToParse);
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("photo");
			Node nodePhoto = nList.item(0);
			Element PhotoElement = (Element) nodePhoto;
			mId=PhotoElement.getAttribute("id");
			NodeList nList2 = PhotoElement.getElementsByTagName("owner");
			Node nodeOwner = nList2.item(0);
			Element OwnerElement = (Element) nodeOwner;
			mUsername = OwnerElement.getAttribute("username");
			mRealname = OwnerElement.getAttribute("realname");
			NodeList nList3 = PhotoElement.getElementsByTagName("title");
			Node nodeTitle = nList3.item(0);
			Element TitleElement = (Element) nodeTitle;
			mTitle =TitleElement.getTextContent().trim();

		} catch (Exception e) {
			Log.e(TAG,"Exception Parsing RPC" + e);
		}
	}

	/**
	 * Get Parsed RPC response ID
	 * @return
	 */
	public String getmId() {
		return mId;
	}
	
	/**
	 * Get Parsed RPC response Username
	 * @return
	 */
	public String getmUsername() {
		return mUsername;
	}
	
	/**
	 * Get Parsed RPC response Realname
	 * @return
	 */
	public String getmRealname() {
		return mRealname;
	}
	
	/**
	 * Get Parsed RPC response Title
	 * @return
	 */
	public String getmTitle() {
		return mTitle;
	}

}
