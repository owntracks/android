package org.owntracks.android.support.interfaces;

import org.owntracks.android.messages.MessageBase;

public interface MessageReceiver {
    void onMessageReceived(MessageBase message);
}
