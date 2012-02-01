/*
 OSC example
 Requires oscP5 (http://www.sojamo.de/libraries/oscP5/index.html)
*/

import oscP5.*;
import netP5.*;
  
OscP5 oscP5;
NetAddress myRemoteLocation;

void setup() {
  size(400,400);
  frameRate(25);
  oscP5 = new OscP5(this, 11000);
  myRemoteLocation = new NetAddress("127.0.0.1", 12000);
}

void draw() {
  background(0);  
}

void mouseMoved() {
  OscMessage myMessage = new OscMessage("/test");
  myMessage.add(mouseX);
  myMessage.add(mouseY);
  myMessage.add("foo");
  oscP5.send(myMessage, myRemoteLocation); 
}

void oscEvent(OscMessage theOscMessage) {
  print(" addrpattern: "+theOscMessage.addrPattern());
  int i = 0;
  for (i = 0; i < theOscMessage.typetag().length(); i++) {
    char c = theOscMessage.typetag().charAt(i);
    if (c == 'i') {
      int d = theOscMessage.get(i).intValue();
      println("[" + i + "] int:" + d);
    } else if (c == 'f') {
      float f = theOscMessage.get(i).floatValue();
      println("[" + i + "] float:" + f);
    } else if (c == 's') {
      String s = theOscMessage.get(i).stringValue();
      println("[" + i + "] String:" + s);
    } else if (c == 'b') {
      byte[] b = theOscMessage.get(i).blobValue(); 
      println("[" + i + "] blob:length:" + b.length);
    }
  }
}

