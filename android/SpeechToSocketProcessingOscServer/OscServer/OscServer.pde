import oscP5.*;
import netP5.*;
import org.apache.commons.codec.binary.Base64;

OscP5 oscP5;
NetAddress myRemoteLocation;
final int INCOMMING_PORT = 57110;
final int OUTGOING_PORT = 57111;
PFont myFont;
ArrayList<String> results = new ArrayList<String>();
final Object lock = new Object();
boolean startRecognition = false;

void setup() {
  size(800, 800);
  frameRate(25);
  oscP5 = new OscP5(this, INCOMMING_PORT);
  
  // WindowsならMS-Pゴシックとか
  myFont = createFont("Osaka", 32);
  
  textFont(myFont);
}


void draw() {
  background(0);
  synchronized(lock) {
    int i = 0;
    if (startRecognition) {
      text("Speak now", 10, 32);
    }
    for (String s : results) {
      text("" + i + ":" + s, 10, (1 + i) * 32);
      i++;
    }
  }
}

void oscEvent(OscMessage theOscMessage) {
  synchronized(lock) {
    print("### received an osc message.");
    String addrPat = theOscMessage.addrPattern();
    print(" addrpattern: "+ addrPat);
    println(" typetag: "+theOscMessage.typetag());
    String tags = theOscMessage.typetag();
    Object[] objs = theOscMessage.arguments();
    println(" length: "+ objs.length);

    String bstr = null;
    boolean isNotify = addrPat.equals("/start");

    for (int i = 0; i < objs.length; i++) {

      OscArgument arg = theOscMessage.get(i);
      if (tags.charAt(i) == 'i') {
        int value = arg.intValue();
        println("" + i + ":" + value);
        if (i == 1 && value == 0) {
          startRecognition = false;
        }
      } 
      else if (tags.charAt(i) == 'f')
        println("" + i + ":" + arg.floatValue());
      else if (tags.charAt(i) == 's')
        println("" + i + ":" + arg.stringValue());
      else if (tags.charAt(i) == 'b') {
        byte[] blob = arg.blobValue();
        byte[] outdata = Base64.decodeBase64(blob);
        try {
          bstr = new String(outdata, "UTF-8");
          results.add(bstr);
          println("" + i + ":" + bstr);
        } 
        catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    }

    if (addrPat.equals("/connect") && bstr != null) {
      myRemoteLocation = new NetAddress(bstr, OUTGOING_PORT);
      println("IP address:" + bstr);
    } 
    else if (addrPat.equals("/start_sr")) {
      startRecognition = true;    
      results.clear();
    }
  }
}

void keyPressed() {
  if (key == ' ') {
    if (myRemoteLocation != null) {
      OscMessage myMessage = new OscMessage("/kick");  
      oscP5.send("/kick_free", new Object[] {
      }
      , myRemoteLocation);
      println("sended");
    }
  }
  else if (key == 'a') {
    if (myRemoteLocation != null) {
      OscMessage myMessage = new OscMessage("/kick");  
      oscP5.send("/kick_web", new Object[] {
      }
      , myRemoteLocation); 
      println("sended");
    }
  }
}

