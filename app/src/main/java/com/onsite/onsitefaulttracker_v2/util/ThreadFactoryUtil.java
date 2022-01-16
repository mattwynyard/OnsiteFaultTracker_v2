package com.onsite.onsitefaulttracker.util;

import android.util.Log;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.Thread.NORM_PRIORITY;

public class ThreadFactoryUtil implements ThreadFactory {

    // The tag name for this class
    private static final String TAG = ThreadFactoryUtil.class.getSimpleName();
    private String name;
    private int priority;
    private AtomicInteger threadNo = new AtomicInteger(0);

    public ThreadFactoryUtil(String name) {
        this.name = name;
        priority = NORM_PRIORITY;
    }

    public ThreadFactoryUtil(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public Thread newThread(Runnable r) {
        String threadName = name + ":" + threadNo.incrementAndGet();
        Thread t = new Thread(r, threadName);
        t.setPriority(priority);
        return t;
    }
}
