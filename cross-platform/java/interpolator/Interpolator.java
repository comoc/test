/**
 * Interpolator
 */
class Interpolator {

	private long mStartTime;
	private long mInterval;
	private boolean mIsLoop;
	private boolean mIsFinished;
	private float mValue;

	public Interpolator() {
	}

	public void setLoop(boolean isLoop) {
		mIsLoop = isLoop;
		mIsFinished = false;
	}

	public void setInterval(long i) {
		mInterval = i;
	}

	public void start(long t) {
		mStartTime = t;
		mIsFinished = false;
		mValue = 0.0f;
	}

	public void update(long t) {
		if (mInterval > 0) {
			if (t > mStartTime) {
				long dt = t - mStartTime;
				if (mIsLoop) {
					mValue = (float)dt / (float)mInterval;
					mValue = mValue - (int)mValue;
				} else {
					if (dt <= mInterval)
						mValue = (float)dt / (float)mInterval;
					else {
						mValue = 1.0f;
						mIsFinished = true;
					}
				}
			} else {
				mValue = 0.0f;
			}
		} else {
			mValue = 0.0f;
		}
	}

	public float getValue() {
		return mValue;
	}

	public boolean finished() {
		return mIsFinished;
	}
}

