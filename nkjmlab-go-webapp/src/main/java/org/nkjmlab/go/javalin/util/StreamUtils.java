package org.nkjmlab.go.javalin.util;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class StreamUtils {

  private StreamUtils() {}

  public static <E> Optional<E> getRandom(Collection<E> e) {
    if (e.size() == 0) {
      return Optional.empty();
    }
    return e.stream().skip((ThreadLocalRandom.current().nextInt(e.size()))).findFirst();
  }
}
