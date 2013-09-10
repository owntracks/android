package st.alr.mqttitude.support;

public interface MqttPublish {
    public void publishSuccessfull(Object extra); 
    public void publishFailed(Object extra);
    public void publishing(Object extra);
    public void publishWaiting(Object extra);
}
