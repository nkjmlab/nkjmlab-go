package org.nkjmlab.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ConcurrentUtils {
  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static List<String> getActiveThreadNames() {
    Thread[] threads = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);
    return Arrays.stream(threads).map((t) -> t.getName()).collect(Collectors.toList());
  }

  public static boolean shutdownAndAwaitTermination(ExecutorService executorService, long timeout,
      TimeUnit unit) {
    executorService.shutdown();
    try {
      log.info("Awaiting shutdown for {} ....", executorService);
      boolean b = executorService.awaitTermination(timeout, unit);
      log.info("{} is shutdown.", executorService);
      return b;
    } catch (InterruptedException e) {
      log.info("Awating shutdown is timeout.");
      log.debug(e.getMessage());
      return false;
    }
  }

  public static void shutdownAndKeepAwaitingTermination(ExecutorService executorService) {
    log.info("Awaiting shutdown for {} ....", executorService);
    executorService.shutdown();
    keepAwaitingTermination(executorService);
    log.info("{} is completely shutdown.", executorService);
  }

  public static void keepAwaitingTermination(ExecutorService executorService) {
    try {
      executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Unexpected interrpution occurred.", e);
    }
  }

  public static List<Runnable> shutdownNowAfterAwaiting(ExecutorService executorService,
      long timeout, TimeUnit unit) {
    if (shutdownAndAwaitTermination(executorService, timeout, unit)) {
      return new ArrayList<>();
    } else {
      return executorService.shutdownNow();
    }
  }

}
