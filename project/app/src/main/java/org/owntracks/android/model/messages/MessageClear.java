package org.owntracks.android.model.messages;

import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;

public class MessageClear extends MessageBase {
    @Override
    public void addMqttPreferences(Preferences preferences) {
        setRetained(true);
    }

    // Clear messages are implemented as empty messages
    @Override
    public byte[] toJsonBytes(Parser parser) {
        return new byte[0];
    }

    @Override
    public String toJson(Parser parser) {
        return "";
    }
}
