package org.nkjmlab.util.lang;

import java.util.concurrent.TimeUnit;

public class ThreadUtils {

  public static void sleep(long timeout, TimeUnit timeUnit) {
    try {
      timeUnit.sleep(timeout);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
