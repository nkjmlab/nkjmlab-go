package org.nkjmlab.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class ForkJoinPoolUtils {
  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static void executeWith(Runnable task) {
    executeWith(getAutoThreadsNum(), task);
  }

  public static void executeWith(int threadsNum, Runnable task) {
    ForkJoinPool pool = new ForkJoinPool(threadsNum);
    pool.execute(task);
    pool.shutdown();
  }

  public static <T> ForkJoinTask<T> submitWith(Callable<T> task) {
    return submitWith(getAutoThreadsNum(), task);
  }

  public static <T> ForkJoinTask<T> submitWith(int threadsNum, Callable<T> task) {
    ForkJoinPool pool = new ForkJoinPool(threadsNum);
    ForkJoinTask<T> result = pool.submit(task);
    pool.shutdown();
    return result;
  }

  public static ForkJoinTask<?> submitWith(Runnable task) {
    return submitWith(getAutoThreadsNum(), task);
  }

  public static ForkJoinTask<?> submitWith(int threadsNum, Runnable task) {
    ForkJoinPool pool = new ForkJoinPool(threadsNum);
    ForkJoinTask<?> result = pool.submit(task);
    pool.shutdown();
    return result;
  }


  public static int getAutoThreadsNumMinus(int threadsNum) {
    if (threadsNum < 0) {
      log.warn("Arg should be eq or greater than 0");
      threadsNum = Math.abs(threadsNum);
    }
    return Math.max(availableProcessors() - threadsNum, 1);
  }

  public static int availableProcessors() {
    return Runtime.getRuntime().availableProcessors();
  }

  public static int getAutoThreadsNum() {
    return getAutoThreadsNumMinus(1);
  }
}
