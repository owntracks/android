package org.owntracks.android.support;

public interface ServiceMqttCallbacks {
	public void publishSuccessfull(Object extra);

	public void publishFailed(Object extra);

	public void publishing(Object extra);

	public void publishWaiting(Object extra);
}
