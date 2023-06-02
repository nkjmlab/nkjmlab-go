package org.nkjmlab.go.javalin.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProblemIdGenerator {

  private final Queue<Long> ids = new ConcurrentLinkedQueue<>();

  public synchronized long getNewId() {
    while (true) {
      long id = System.currentTimeMillis();
      if (!ids.contains(id)) {
        ids.offer(id);
        if (ids.size() > 1000) {
          ids.poll();
        }
        return id;
      }
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
      }
    }
  }
}
