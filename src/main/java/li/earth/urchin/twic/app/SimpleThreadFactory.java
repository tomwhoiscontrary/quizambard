package li.earth.urchin.twic.app;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private final ThreadGroup group;
    private final String namePrefix;
    private final AtomicInteger counter = new AtomicInteger(1);
    private final boolean daemon;
    private final int priority;

    public SimpleThreadFactory(String namePrefix) {
        this(namePrefix, false);
    }

    public SimpleThreadFactory(String namePrefix, boolean daemon) {
        this(namePrefix, daemon, Thread.currentThread().getPriority());
    }

    public SimpleThreadFactory(String namePrefix, boolean daemon, int priority) {
        SecurityManager securityManager = System.getSecurityManager();
        ThreadGroup ambientGroup = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        group = new ThreadGroup(ambientGroup, namePrefix);
        this.namePrefix = namePrefix;
        this.daemon = daemon;
        this.priority = priority;
    }

    public Thread newThread(Runnable target) {
        Thread thread = new Thread(group,
                                   target,
                                   namePrefix + "-" + counter.getAndIncrement(),
                                   0);
        thread.setDaemon(daemon);
        thread.setPriority(priority);
        return thread;
    }

}
