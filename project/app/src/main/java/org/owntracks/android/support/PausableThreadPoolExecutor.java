package org.owntracks.android.support;

import org.owntracks.android.messages.MessageBase;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableThreadPoolExecutor extends ThreadPoolExecutor {
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
        super.execute(command);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
    }
    /**
     * @param thread   The thread being executed
     * @param runnable The runnable task
     * @see {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     */
    @Override
    protected void beforeExecute(Thread thread, Runnable runnable)  {

        super.beforeExecute(thread, runnable);
        lock.lock();
        try {
            while (isPaused) condition.await();
        } catch (InterruptedException ie) {
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

        if(isPaused) {
            return;
        }

        lock.lock();
        try {
            isPaused = true;
        } finally {
            lock.unlock();
        }

    }

    /**
     * Resume pool execution
     */
    public void resume() {
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
        this.execute(message);
    }

    public void requeue(MessageBase message) {
        this.queue(message);
    }

    public interface ExecutorRunnable extends java.lang.Runnable{
        void cancelOnRun();
    }

    public int getQueueLength() {
        return getQueue().size();
    }


}