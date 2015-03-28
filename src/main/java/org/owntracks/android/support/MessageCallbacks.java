package org.owntracks.android.support;

public interface MessageCallbacks {
	public void publishSuccessfull(Object extra, boolean wasQueued);

	public void publishFailed(Object extra);

	public void publishing(Object extra);

    public void publishQueued(Object extra);

}
