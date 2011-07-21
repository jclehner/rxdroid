package at.caspase.rxdroid;

public class Timer
{
	private long mBegin;
	
	public Timer() {
		reset();
	}
	
	public void reset() {
		mBegin = System.currentTimeMillis();
	}
	
	public long elapsed() {
		return System.currentTimeMillis() - mBegin;
	}
	
	public double elapsedSeconds() {
		return (double) elapsed() / 1000;
	}
	
	@Override
	public String toString() {
		return elapsedSeconds() + "s";
	}

}
