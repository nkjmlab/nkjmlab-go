package org.nkjmlab.util.concurrent;

import java.util.concurrent.ThreadFactory;

public class NamedSingleThreadFactory implements ThreadFactory {

  private final String threadName;
  private final boolean daemon;
  private final int priority;

  public NamedSingleThreadFactory(String threadName) {
    this(threadName, false, Thread.NORM_PRIORITY);
  }

  public NamedSingleThreadFactory(String threadName, boolean daemon) {
    this(threadName, daemon, Thread.NORM_PRIORITY);
  }

  public NamedSingleThreadFactory(String threadName, boolean daemon, int priority) {
    this.threadName = threadName;
    this.daemon = daemon;
    this.priority = priority;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, threadName);
    if (daemon) {
      t.setDaemon(true);
    } else {
      t.setDaemon(false);
    }
    t.setPriority(priority);
    return t;
  }
}
