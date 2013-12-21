package st.alr.mqttitude.support;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;
import st.alr.mqttitude.support.StaticHandlerInterface;

public class StaticHandler extends Handler {
    private final WeakReference<StaticHandlerInterface> ref; 

    public StaticHandler(StaticHandlerInterface caller) {
        ref = new WeakReference<StaticHandlerInterface>(caller);
    }
    @Override
    public void handleMessage(Message msg)
    {
        StaticHandlerInterface caller = ref.get();
         if (caller != null) {
              caller.handleHandlerMessage(msg);
         }
    }
}
