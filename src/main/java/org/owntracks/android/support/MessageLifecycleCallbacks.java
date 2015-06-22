package org.owntracks.android.support;

public interface MessageLifecycleCallbacks {
	public void onMessagePublishSuccessful(Object extra, boolean wasQueued);

	public void onMessagePublishFailed(Object extra);

	public void onMesssagePublishing(Object extra);

    public void onMessagePublishQueued(Object extra);

}
