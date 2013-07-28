package st.alr.mqttitude.support;

public interface MqttPublish {
    public void publishSuccessfull(); 
    public void publishTimeout();
    public void publishing();
    public void waiting();
}
