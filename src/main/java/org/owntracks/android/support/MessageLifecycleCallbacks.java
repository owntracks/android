package org.owntracks.android.support;

public interface MessageLifecycleCallbacks {
	void onMessagePublishSuccessful(Object extra, boolean wasQueued);

	void onMessagePublishFailed(Object extra);

	void onMesssagePublishing(Object extra);

    void onMessagePublishQueued(Object extra);

}
