package org.owntracks.android.support;


        import android.util.Log;

        import org.owntracks.android.messages.MessageBase;

        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.ThreadPoolExecutor;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.locks.Condition;
        import java.util.concurrent.locks.ReentrantLock;

/**
 * A light wrapper around the {@link ThreadPoolExecutor}. It allows for you to pause execution and
 * resume execution when ready. It is very handy for games that need to pause.
 *
 * @author Matthew A. Johnston (warmwaffles)
 */
public class PausableThreadPoolExecutor extends ThreadPoolExecutor {
    private static final String TAG = "ThreadPoolExecutor";
    private boolean isPaused;
    private final ReentrantLock lock;
    private final Condition condition;



    /**
     * @param corePoolSize    The size of the pool
     * @param maximumPoolSize The maximum size of the pool
     * @param keepAliveTime   The amount of time you wish to keep a single task alive
     * @param unit            The unit of time that the keep alive time represents
     * @param workQueue       The queue that holds your tasks
     * @see {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue)}
     */
    public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        lock = new ReentrantLock();
        condition = lock.newCondition();
    }

    public void execute(Runnable command) {
        Log.v(TAG, "queued() " + command+ " executor: " + this);
        super.execute(command);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        Log.v(TAG, "afterRun() " + r+ " executor: " + this);
        super.afterExecute(r, t);
    }
    /**
     * @param thread   The thread being executed
     * @param runnable The runnable task
     * @see {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)  {
        Log.v(TAG, "beforeRun() " + runnable + " executor: " + this);

        super.beforeExecute(thread, runnable);
        lock.lock();
        try {
            while (isPaused) condition.await();
        } catch (InterruptedException ie) {
            Log.v(TAG, "InterruptedException " + runnable + " executor: " + this);
            if(runnable instanceof CanceableRunnable)
                ((CanceableRunnable)runnable).cancelOnRun();
            thread.interrupt();
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        return !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Pause the execution
     */
    public void pause() {
        Log.v(TAG, "pause() isPaused:" +isPaused + " executor: " + this) ;

        if(isPaused) {
            Log.v(TAG, "already paused" + " executor: " + this);
            return;
        } else {
            Log.v(TAG, "pausing" + " executor: " + this);
        }

        lock.lock();
        try {
            isPaused = true;
        } finally {
            lock.unlock();
        }
        Log.v(TAG, "paused" + " executor: " + this);

    }

    /**
     * Resume pool execution
     */
    public void resume() {
        Log.v(TAG, "resume" + " executor: " + this);
        if(!isPaused)
            return;

        lock.lock();
        try {
            isPaused = false;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void queue(MessageBase message) {
        Log.v(TAG, "queue message");

        this.execute(message);
    }

    public void requeue(MessageBase message) {
        Log.v(TAG, "requeueing message");
        this.queue(message);
    }

    public interface ExecutorRunnable extends java.lang.Runnable{
        void cancelOnRun();
    }

    public int getQueueLength() {
        return getQueue().size();
    }


}