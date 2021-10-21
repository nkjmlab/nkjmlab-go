package org.nkjmlab.util.lang;

import java.io.IOException;
import org.apache.logging.log4j.Logger;

public class RuntimeUtils {
  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static String getMemoryUsege() {
    final Runtime runtime = Runtime.getRuntime();
    return String.format("{total: %d MB, free: %d MB, used: %d MB, max: %d MB}",
        (runtime.totalMemory() / 1000 / 1000), (runtime.freeMemory() / 1000 / 1000),
        ((runtime.totalMemory() - runtime.freeMemory()) / 1000 / 1000),
        (runtime.maxMemory() / 1000 / 1000));
  }

  public static String getPid() {
    return java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }


  public static void callSystemExitWhenReadingSystemInput() {
    try {
      System.out.println("Press ENTER key to call System.exit() in the console ... > ");
      System.in.read();
    } catch (IOException e) {
      log.error(e, e);
    } finally {
      log.info("System.exit(0) call immediately.");
      System.exit(0);
    }
  }

  public static void addShutdownLog(Logger log, String msg, Object... params) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      String message = "[SHUTDOWN HOOK]" + MessageUtils.format(msg, params);
      log.error(message);
      System.err.println(message); // This line is important for flash Logger I guess.
      org.apache.logging.log4j.LogManager.shutdown();
    }));
  }


}
