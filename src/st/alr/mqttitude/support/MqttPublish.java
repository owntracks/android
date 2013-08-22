package st.alr.mqttitude.support;

public interface MqttPublish {
    public void publishSuccessfull(); 
    public void publishFailed();
    public void publishing();
    public void publishWaiting();
}
