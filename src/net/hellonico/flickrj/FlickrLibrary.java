/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package net.hellonico.flickrj;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import processing.core.PApplet;
import processing.core.PImage;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.AuthInterface;
import com.aetrion.flickr.auth.Permission;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;

/**
 * This is a template class and can be used to start a new processing library or tool.
 * Make sure you rename this class as well as the name of the example package 'template' 
 * to your own library or tool naming convention.
 * 
 * @example Hello 
 * 
 * (the tag @example followed by the name of an example included in folder 'examples' will
 * automatically include the example in the javadoc.)
 *
 */

public class FlickrLibrary {
	
	PApplet myParent;
	Auth auth;
	Flickr flickr;
	Uploader uploader;
	
	// Fill in your own apiKey and secretKey values.
	String apiKey = "3adbd93e465941c9836c19d31b5e1669";
	String secretKey = "0a494fee1d843f15";
	String frob = "";
	String token = "";
	
	
	public final static String VERSION = "##library.prettyVersion##";
	
	public FlickrLibrary(PApplet theParent) {
		myParent = theParent;
		flickr = new Flickr(apiKey, secretKey, (new Flickr(apiKey)).getTransport());
		authenticate();
	}
	
	public static String version() {
		return VERSION;
	}
	

	// Attempts to authenticate. Note this approach is bad form,
	// it uses side effects, etc.
	void authenticate() {
	  // Do we already have a token?
	  if (fileExists("token.txt")) {
	    token = loadToken();    
	    myParent.println("Using saved token " + token);
	    authenticateWithToken(token);
	  }
	  else {
	   myParent.println("No saved token. Opening browser for authentication");    
	   getAuthentication();
	  }
	}
	
	// FLICKR AUTHENTICATION HELPER FUNCTIONS
	// Attempts to authneticate with a given token
	void authenticateWithToken(String _token) {
	  AuthInterface authInterface = flickr.getAuthInterface();  
	  
	  // make sure the token is legit
	  try {
	    authInterface.checkToken(_token);
	  }
	  catch (Exception e) {
	    myParent.println("Token is bad, getting a new one");
	    getAuthentication();
	    return;
	  }
	  
	  auth = new Auth();

	  RequestContext requestContext = RequestContext.getRequestContext();
	  requestContext.setSharedSecret(secretKey);    
	  requestContext.setAuth(auth);
	  
	  auth.setToken(_token);
	  auth.setPermission(Permission.WRITE);
	  flickr.setAuth(auth);
	  myParent.println("Authentication success");
	}
	

	// Load the token string from a file
	String loadToken() {
	  String[] toRead = myParent.loadStrings("token.txt");
	  return toRead[0];
	}



	// Goes online to get user authentication from Flickr.
	void getAuthentication() {
	  AuthInterface authInterface = flickr.getAuthInterface();
	  
	  try {
	    frob = authInterface.getFrob();
	  } 
	  catch (Exception e) {
	    e.printStackTrace();
	  }

	  try {
	    URL authURL = authInterface.buildAuthenticationUrl(Permission.WRITE, frob);
	    
	    // open the authentication URL in a browser
	    myParent.open(authURL.toExternalForm());    
	  }
	  catch (Exception e) {
	    e.printStackTrace();
	  }

	  myParent.println("You have 15 seconds to approve the app!");  
	  int startedWaiting = myParent.millis();
	  int waitDuration = 15 * 1000; // wait 10 seconds  
	  while ((myParent.millis() - startedWaiting) < waitDuration) {
	    // just wait
	  }
	  myParent.println("Done waiting");

	  try {
	    auth = authInterface.getToken(frob);
	    myParent.println("Authentication success");
	    // This token can be used until the user revokes it.
	    token = auth.getToken();
	    // save it for future use
	    saveToken(token);
	  }
	  catch (Exception e) {
	    e.printStackTrace();
	  }
	  
	  // complete authentication
	  authenticateWithToken(token);
	}

	// Writes the token to a file so we don't have
	// to re-authenticate every time we run the app
	void saveToken(String _token) {
	  String[] toWrite = { _token };
	  myParent.saveStrings("token.txt", toWrite);  
	}

	boolean fileExists(String filename) {
	  File file = new File(myParent.sketchPath(filename));
	  return file.exists();
	}
	
	public void upload(PImage cam,
			String title, 
			String description,
			boolean makePublic
			) {
		  myParent.println("Uploading");
		  
		  // First compress it as a jpeg.
		  byte[] compressedImage = compressImage(cam);
		  
		  // Set some meta data.
		  UploadMetaData uploadMetaData = new UploadMetaData(); 
		  uploadMetaData.setTitle(title); 
		  uploadMetaData.setDescription(description);   
		  uploadMetaData.setPublicFlag(makePublic);
		  //uploadMetaData.setTags(tags);

		  // Finally, upload/
		  try {
			uploader = flickr.getUploader();
		    uploader.upload(compressedImage, uploadMetaData);
		  }
		  catch (Exception e) {
			  e.printStackTrace();
			  myParent.println("Upload failed");
		  }
		  
		  myParent.println("Finished uploading");  
	}
	

	// IMAGE COMPRESSION HELPER FUNCTION
	// TODO: move me out of here

	// Takes a PImage and compresses it into a JPEG byte stream
	// Adapted from Dan Shiffman's UDP Sender code
	byte[] compressImage(PImage img) {
	  // We need a buffered image to do the JPG encoding
	  BufferedImage bimg = new BufferedImage( img.width,img.height, BufferedImage.TYPE_INT_RGB );

	  img.loadPixels();
	  bimg.setRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);

	  // Need these output streams to get image as bytes for UDP communication
	  ByteArrayOutputStream baStream	= new ByteArrayOutputStream();
	  BufferedOutputStream bos		= new BufferedOutputStream(baStream);

	  // Turn the BufferedImage into a JPG and put it in the BufferedOutputStream
	  // Requires try/catch
	  try {
	    ImageIO.write(bimg, "jpg", bos);
	  } 
	  catch (IOException e) {
	    e.printStackTrace();
	  }

	  // Get the byte array, which we will send out via UDP!
	  return baStream.toByteArray();
	}


}

