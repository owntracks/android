
package st.alr.mqttitude.support;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

public class StaticHandler extends Handler {
    private final WeakReference<StaticHandlerInterface> ref;

    public StaticHandler(StaticHandlerInterface caller) {
        this.ref = new WeakReference<StaticHandlerInterface>(caller);
    }

    @Override
    public void handleMessage(Message msg)
    {
        StaticHandlerInterface caller = this.ref.get();
        if (caller != null) {
            caller.handleHandlerMessage(msg);
        }
    }
}
