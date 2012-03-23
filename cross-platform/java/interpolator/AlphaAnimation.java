/**
 * Alpha animation
 */
class AlphaAnimation {
	private Interpolator mInterpolator;
	private int mAlphaFrom;
	private int mAlphaTo;
	private int mAlpha;
	public AlphaAnimation() {
		mInterpolator = new Interpolator();
		mInterpolator.setLoop(false);
	}

	private int clamp(int a) {
		if (a < 0)
			a = 0;
		else if (a > 0xff)
			a = 0xff;
		return a;
	}

	public void start(int alphaFrom, int alphaTo, long duration, long time) {
		mAlphaFrom = clamp(alphaFrom);
		mAlphaTo = clamp(alphaTo);
		mInterpolator.setInterval(duration);
		mInterpolator.start(time);
	}
	
	public void startFromCurrentValue(int alphaTo, long duration, long time) {
		mAlphaFrom = mAlpha;
		mAlphaTo = clamp(alphaTo);
		mInterpolator.setInterval(duration);
		mInterpolator.start(time);
	}

	public void update(long time) {
		mInterpolator.update(time);
		float v = mInterpolator.getValue();
		mAlpha = (int)((mAlphaTo - mAlphaFrom) * v) + mAlphaFrom;
	}

	public int getValue() {
		return mAlpha;
	}

	boolean finished() {
		return mInterpolator.finished();
	}
}

