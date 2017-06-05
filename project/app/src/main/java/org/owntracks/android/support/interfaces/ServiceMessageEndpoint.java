package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.services.MessageProcessor;

public interface ServiceMessageEndpoint extends ProxyableService {
        void onSetService(MessageProcessor service);
        boolean sendMessage(MessageBase message);

}
