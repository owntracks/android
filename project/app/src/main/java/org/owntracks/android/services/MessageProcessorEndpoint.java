package org.owntracks.android.services;

import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public abstract class MessageProcessorEndpoint implements OutgoingMessageProcessor {
    MessageProcessor messageProcessor;
    BlockingDeque<MessageBase> outgoingMessageQueue;

    MessageProcessorEndpoint(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    void onMessageReceived(MessageBase message) {
        message.setModeId(getModeId());
        onFinalizeMessage(message).processIncomingMessage(messageProcessor);
    }

    protected abstract MessageBase onFinalizeMessage(MessageBase message);

    abstract int getModeId();

    abstract void sendMessage(MessageBase m) throws ConfigurationIncompleteException, OutgoingMessageSendingException, IOException;

    public Runnable getBackgroundOutgoingRunnable() {
        return this::sendAvailableMessages;
    }

    private void sendAvailableMessages() {
        Timber.tag("outgoing").v("Starting outbound message loop. ThreadID: %s", Thread.currentThread());
        MessageBase lastFailedMessageToBeRetried = null;
        while (true) {
            try {
                MessageBase message;
                if (lastFailedMessageToBeRetried == null) {
                    message = outgoingMessageQueue.take();
                } else {
                    message = lastFailedMessageToBeRetried;
                }
                try {
                    sendMessage(message);
                    lastFailedMessageToBeRetried = null;
                } catch (OutgoingMessageSendingException | ConfigurationIncompleteException e) {
                    Timber.tag("outgoing").w(("Error sending message. Re-queueing"));
                    lastFailedMessageToBeRetried = message;
                } catch (IOException e) {
                    // Deserialization failure, drop and move on
                }
                if (lastFailedMessageToBeRetried != null) {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1)); //TODO: better backoff algorithm
                }
            } catch (InterruptedException e) {
                Timber.tag("outgoing").i(e, "Outgoing message loop interrupted");
                break;
            }
        }
        Timber.tag("outgoing").w("Exiting outgoingmessage loop");
    }
}

class OutgoingMessageSendingException extends Throwable {
    OutgoingMessageSendingException(Exception e) {
        super(e);
    }
}
