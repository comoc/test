import processing.serial.*;

Serial myPort;
final Object lock = new Object();
ArrayList<String> results = new ArrayList<String>();


void setup(){
  size(640, 480);
  myPort=new Serial(this,"/dev/tty.NexusOne-SpeechServer",9600);
  
  PFont font = createFont("Osaka", 32);
  textFont(font);  
}


void draw(){
  background(0);
  synchronized(lock) {
    int i = 0;
    for (String s : results) {
      text(s, 10, 32 * (i + 1));
      i++;
    }
  }
}

void keyPressed() {
  if (key == 'f') {
    myPort.write('f');
  } else if (key == 'w') {
    myPort.write('w');
  }
}

void serialEvent(Serial p){
  byte[] b = myPort.readBytesUntil('\n');
  println("bytes:" + b);
  if (b == null)
    return;
    
  try {
    String inString = new String(b, "UTF-8");
    inString.substring(0, inString.length() - 1);
    println(inString);
    synchronized(lock) {
    if (results.size() > 10)
      results.remove(0);
      results.add(inString);
    }
  } catch (UnsupportedEncodingException e) {
    e.printStackTrace();
  }
    
}
