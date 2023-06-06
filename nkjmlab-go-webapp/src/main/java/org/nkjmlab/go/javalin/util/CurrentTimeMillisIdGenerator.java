package org.nkjmlab.go.javalin.util;

import java.util.concurrent.TimeUnit;

public class CurrentTimeMillisIdGenerator {

  private volatile long prev = -1;

  public synchronized long getNewId() {
    while (true) {
      long id = System.currentTimeMillis();
      if (prev == id) {
        try {
          TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException e) {
        }
        continue;
      }
      prev = id;
      return id;
    }
  }
}
