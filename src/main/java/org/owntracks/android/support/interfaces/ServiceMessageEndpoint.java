package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.services.ServiceMessage;

public interface ServiceMessageEndpoint extends ProxyableService {
        void onSetService(ServiceMessage service);
        boolean sendMessage(MessageBase message);
        boolean isReady();
}
