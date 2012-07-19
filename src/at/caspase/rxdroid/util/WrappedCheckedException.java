package at.caspase.rxdroid.util;

public class WrappedCheckedException extends RuntimeException
{
	private static final long serialVersionUID = 7323462591621207126L;

	public WrappedCheckedException(Exception ex) {
		super(ex);
	}

	public WrappedCheckedException(String detailMessage, Exception ex) {
		super(detailMessage, ex);
	}

	/*public WrappedCheckedException(WrappedCheckedException ex) {
		super(ex.getCause());
	}

	public WrappedCheckedException(String detailMessage, WrappedCheckedException ex) {
		super(detailMessage, ex.getCause());
	}*/

	@Override
	public Throwable getCause() {
		return getFirstWrappedCause();
	}

	@Override
	public String toString()
	{
		final Throwable cause = getFirstWrappedCause();
		if(cause == null)
			return super.toString();

		return cause.toString();
	}

	public Throwable getFirstWrappedCause()
	{
		Throwable t = super.getCause();

		//while(t != null && t instanceof WrappedCheckedException)
		//	t = t.getCause();

		if(t != null && t instanceof WrappedCheckedException)
			return t.getCause();

		return t;
	}

	public Class<?> getCauseType()
	{
		final Throwable cause = getCause();
		return cause == null ? null : cause.getClass();
	}

}
