package org.nkjmlab.util.lang;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

  public static String getMessageWithStackTrace(Throwable t) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    t.printStackTrace(pw);
    return sw.getBuffer().toString();
  }

  public static String getMessageWithStackTrace(Throwable t, int maxStackDepth,
      int maxTotalStackDepth) {
    return getMessageWithStackTrace(t);

  }


}
