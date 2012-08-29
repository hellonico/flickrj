import processing.video.*;
import net.hellonico.flickrj.*;

FlickrLibrary flickr;
Capture cam;

void setup() {
  size(320, 240);

  cam = new Capture(this, 320, 240);  
  cam.start();

  flickr = new FlickrLibrary(this);
}

void draw() {
  if (cam.available()) {
    cam.read();
    image(cam, 0, 0);
    text("Click to upload to Flickr", 10, height - 13);
  }
}

void mousePressed() {
  flickr.upload(cam, "Title", "Description", false);
}

