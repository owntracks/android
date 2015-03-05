package org.owntracks.android.support;

public interface MessageCallbacks {
	public void publishSuccessfull(Object extra);

	public void publishFailed(Object extra);

	public void publishing(Object extra);

	public void publishWaiting(Object extra);
}
