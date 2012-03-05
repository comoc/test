// http://www.sojamo.de/libraries/oscP5/reference/index.html
import oscP5.*;
import netP5.*;
  
OscP5 oscP5;
NetAddress myRemoteLocation;

void setup() {
  size(400,400);
  frameRate(25);
  oscP5 = new OscP5(this, 12000);
  myRemoteLocation = new NetAddress("127.0.0.1", 11000);
}


void draw() {
  background(0);  
}

void mousePressed() {
  OscMessage myMessage = new OscMessage("/oscmessage");
  myMessage.add("123");
  oscP5.send(myMessage, myRemoteLocation); 
  println("/oscmessage");
//  OscBundle myBundle = new OscBundle();
//  myBundle.add(myMessage);
//  myBundle.setTimetag(myBundle.now() + 10000);
//  oscP5.send(myBundle, myRemoteLocation);
}

void oscEvent(OscMessage theOscMessage) {
  println("### received an osc message.");
  println(" addrpattern: "+ theOscMessage.addrPattern());
  println(" typetag: "+ theOscMessage.typetag());
  String tt = theOscMessage.typetag();
  for (int i = 0; i < tt.length(); i++) {
    char c = tt.charAt(i);
    OscArgument arg = theOscMessage.get(i);
    println(" messge: " + i + " : " + arg.toString());
  }
}
