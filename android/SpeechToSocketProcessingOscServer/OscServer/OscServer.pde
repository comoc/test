import oscP5.*;
import netP5.*;
import org.apache.commons.codec.binary.Base64;
  
OscP5 oscP5;

void setup() {
  size(400,400);
  frameRate(25);
  oscP5 = new OscP5(this,57110);
}


void draw() {
  background(0);  
}

void oscEvent(OscMessage theOscMessage) {
  print("### received an osc message.");
  print(" addrpattern: "+theOscMessage.addrPattern());
  println(" typetag: "+theOscMessage.typetag());
  String tags = theOscMessage.typetag();
  Object[] objs = theOscMessage.arguments();
  println(" length: "+ objs.length);
  
  for (int i = 0; i < objs.length; i++) {
    OscArgument arg = theOscMessage.get(i);
    if (tags.charAt(i) == 'i')
      println("" + i + ":" + arg.intValue());
    else if (tags.charAt(i) == 'f')
      println("" + i + ":" + arg.floatValue());
    else if (tags.charAt(i) == 's')
      println("" + i + ":" + arg.stringValue());
    else if (tags.charAt(i) == 'b') {
      byte[] blob = arg.blobValue();
      byte[] outdata = Base64.decodeBase64(blob);
      try {
        println("" + i + ":" + new String(outdata, "UTF-8"));      
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }
  }
}
