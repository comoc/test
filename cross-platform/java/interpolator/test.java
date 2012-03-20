class test {
	public static void main(String[] args) {
		AlphaAnimation i = new AlphaAnimation();
		i.start(0x80, 0xff, 1000, System.currentTimeMillis());
		int count = 0;
		while (count <= 2) {

			try{
				i.update(System.currentTimeMillis());
				int a = i.getValue();
				System.out.println("" + a);
				Thread.sleep(200);
			}catch(InterruptedException e){
				break;
			}
			if (i.finished()) {
				if (count == 0)
					i.startFromCurrentValue(0x10, 1000, System.currentTimeMillis());
				else if (count == 1)
					i.startFromCurrentValue(0x0, 1000, System.currentTimeMillis());
				count ++;
			}
		}
	}
}
