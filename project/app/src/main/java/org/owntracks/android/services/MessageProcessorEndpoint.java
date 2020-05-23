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
    BlockingDeque<MessageBase> outgoingMessageQueue;

    private static final long SEND_FAILURE_BACKOFF_INITIAL_WAIT = TimeUnit.SECONDS.toMillis(1);
    private static final long SEND_FAILURE_BACKOFF_MAX_WAIT = TimeUnit.MINUTES.toMillis(1);


    private RunThingsOnOtherThreads runThingsOnOtherThreads;

    MessageProcessorEndpoint(MessageProcessor messageProcessor,RunThingsOnOtherThreads runThingsOnOtherThreads) {
        this.messageProcessor = messageProcessor;
        this.runThingsOnOtherThreads = runThingsOnOtherThreads;
    }

    void onMessageReceived(MessageBase message) {
        message.setIncoming();
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
        Timber.tag("outgoing").d("Starting outbound message loop. ThreadID: %s", Thread.currentThread());
        MessageBase lastFailedMessageToBeRetried = null;
        long retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
        while (true) {
            try {
                MessageBase message;
                if (lastFailedMessageToBeRetried == null) {
                    message = outgoingMessageQueue.take();
                } else {
                    message = lastFailedMessageToBeRetried;
                }

                /*
                We need to run the actual network sending part on a different thread because the
                implementation might not be thread-safe. So we wrap `sendMessage()` up in a callable
                and a FutureTask and then dispatch it off to the network thread, and block on the
                return, handling any exceptions that might have been thrown.
                */
                Callable<Void> sendMessageCallable = () -> {
                    this.sendMessage(message);
                    return null;
                };
                FutureTask<Void> futureTask = new FutureTask<>(sendMessageCallable);
                runThingsOnOtherThreads.postOnNetworkHandlerDelayed(futureTask,1);
                try {
                    try {
                        futureTask.get();
                    } catch (ExecutionException e) {
                        if (e.getCause()!=null) {
                            throw e.getCause();
                        } else {
                            throw new Exception("sendMessage failed, but no exception actually given");
                        }
                    }
                    lastFailedMessageToBeRetried = null;
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                } catch (OutgoingMessageSendingException | ConfigurationIncompleteException e) {
                    Timber.tag("outgoing").w(("Error sending message. Re-queueing"));
                    lastFailedMessageToBeRetried = message;
                } catch (IOException e) {
                    retryWait = SEND_FAILURE_BACKOFF_INITIAL_WAIT;
                    // Deserialization failure, drop and move on
                } catch(Throwable e) {
                    Timber.tag("outgoing").e(e,"Unhandled exception in sending message");
                }
                if (lastFailedMessageToBeRetried != null) {
                    Thread.sleep(retryWait);
                    retryWait = Math.min(2 * retryWait, SEND_FAILURE_BACKOFF_MAX_WAIT);
                }
            } catch (InterruptedException e) {
                Timber.tag("outgoing").i(e, "Outgoing message loop interrupted");
                break;
            }
        }
        Timber.tag("outgoing").w("Exiting outgoingmessage loop");
    }
}

class OutgoingMessageSendingException extends Exception {
    OutgoingMessageSendingException(Exception e) {
        super(e);
    }
}
