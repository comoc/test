import java.awt.*;
import java.awt.event.*;
import java.io.*;

class StdinToRobot {

	public static boolean isInBounds(int x, int y) {

//		Rectangle virtualBounds = new Rectangle();
		GraphicsEnvironment ge = GraphicsEnvironment.
			getLocalGraphicsEnvironment();
		GraphicsDevice[] gs =
			ge.getScreenDevices();
		for (int j = 0; j < gs.length; j++) { 
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration[] gc =
				gd.getConfigurations();
			for (int i=0; i < gc.length; i++) {
				if (gc[i].getBounds().contains(x, y))
					return true;
//				virtualBounds =
//					virtualBounds.union(gc[i].getBounds());
			}
		}

		return false;	
	}

	public static void main(String[] args) {
		Robot robot;
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
			return;
		}

		BufferedReader stdin = new BufferedReader
      		(new InputStreamReader(System.in));
		try {
			while (true) {
				String s = stdin.readLine();
//				System.out.println(StdinToRobot.class.getName() + " in:" + s);
//				System.out.println(StdinToRobot.class.getName() + " touch:" + s.indexOf("TOUC"));
//				System.out.println(StdinToRobot.class.getName() + " tap:" + s.indexOf("TAP_"));
                
				if (s.indexOf("TOUC") == 0) {
					String[] stars = s.split(",");
					if (stars.length >= 3) {

						PointerInfo a = MouseInfo.getPointerInfo();
						if (a != null) {
							Point b  = a.getLocation();
							int dx = Integer.parseInt(stars[1]);
							int dy = Integer.parseInt(stars[2]);
							int x = (int)b.getX() - dx;
							int y = (int)b.getY() - dy;
//							if (x < 0)
//								x = 0;
//							else if (x >= 4096)
//								x = 4095;
//							if (y < 0)
//								y = 0;
//							else if (y >= 4096)
//								y = 4095;
							System.out.println(StdinToRobot.class.getName() + "MOVE x:"+ x + " y:" + y);
							if (isInBounds(x, y)) {
								robot.mouseMove(x, y);
							}
						}
					}
				} else if (s.indexOf("TAP_") == 0) {
					System.out.println(StdinToRobot.class.getName() + "TAP");
					robot.mousePress(InputEvent.BUTTON1_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_MASK);
//				} else if (s.indexOf("DIRU") == 0) {
//					robot.keyPress(KeyEvent.VK_PAGE_DOWN);
//					robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
//					robot.mouseWheel(5);
//        		} else if (s.indexOf("DIRD") == 0) {
//					robot.keyPress(KeyEvent.VK_PAGE_UP);
//					robot.keyRelease(KeyEvent.VK_PAGE_UP);
//					robot.mouseWheel(-5);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
