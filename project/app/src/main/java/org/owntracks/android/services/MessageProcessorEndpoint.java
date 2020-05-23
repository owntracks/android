package org.owntracks.android.services;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import timber.log.Timber;

public abstract class MessageProcessorEndpoint implements OutgoingMessageProcessor {
    MessageProcessor messageProcessor;

    MessageProcessorEndpoint(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    void onMessageReceived(MessageBase message) {
        message.setIncoming();
        message.setModeId(getModeId());
        onFinalizeMessage(message).processIncomingMessage(messageProcessor);
    }

    protected abstract MessageBase onFinalizeMessage(MessageBase message);

    abstract int getModeId();

    abstract void sendMessage(MessageBase m) throws ConfigurationIncompleteException, OutgoingMessageSendingException, IOException;
}

class OutgoingMessageSendingException extends Exception {
    OutgoingMessageSendingException(Exception e) {
        super(e);
    }
}
