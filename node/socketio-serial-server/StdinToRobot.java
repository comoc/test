import java.awt.*;
import java.awt.event.*;
import java.io.*;

class StdinToRobot {

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
						int dx = Integer.parseInt(stars[1]);
						int dy = Integer.parseInt(stars[2]);

						PointerInfo a = MouseInfo.getPointerInfo();
						Point b  = a.getLocation();
						int x = (int)b.getX() - dx;
						int y = (int)b.getY() - dy;
						System.out.println(StdinToRobot.class.getName() + "MOVE x:"+ x + " y:" + y);
						robot.mouseMove(x, y);
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
