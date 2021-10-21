package org.nkjmlab.util.lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtils {
  public static String trimLast(String str, char x) {
    if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == x) {
      str = str.substring(0, str.length() - 1);
    }
    return str;
  }

  public static String join(String delimiter, Object... elements) {
    String[] sElements = new String[elements.length];

    for (int i = 0; i < elements.length; i++) {
      Object o = elements[i];
      sElements[i] = o == null ? "null" : o.toString();
    }

    return String.join(delimiter, sElements);
  }


  public static void main(String[] args) {
    System.out.println(getStringList("A", "Z"));
    System.out.println(getStringList("A", "z"));
    System.out.println(getStringList("A", 2));
    System.out.println(getStringList("A", 26));
    System.out.println(getStringList("a", "z"));
    System.out.println(getStringList("あ", "ん"));
    System.out.println(getStringList("ア", "ン"));

  }


  /**
   * https://ja.wikipedia.org/wiki/Unicode%E4%B8%80%E8%A6%A7_0000-0FFF
   *
   * @param from
   * @param to
   * @return
   */
  public static List<String> getStringList(String from, String to) {
    char fc = from.toCharArray()[0];
    char tc = to.toCharArray()[0];
    return getStringList(fc, tc);
  }

  public static List<String> getStringList(char from, char to) {
    return IntStream.rangeClosed(from, to).mapToObj(i -> String.valueOf((char) i))
        .collect(Collectors.toList());
  }

  public static List<String> getStringList(String from, int num) {
    char fc = from.toCharArray()[0];
    return getStringList(fc, (char) (fc + num - 1));

  }

  public static final List<String> upperAlphabets =
      Collections.unmodifiableList(getStringList("A", "Z"));
  public static final List<String> lowerAlphabets =
      Collections.unmodifiableList(getStringList("a", "z"));

  public static String toUpperAlphabet(int order) {
    return upperAlphabets.get(order);
  }

  public static String toLowerAlphabet(int order) {
    return lowerAlphabets.get(order);
  }

  public static String format(String msg, Object... params) {
    if (params == null || params.length == 0) {
      return msg;
    }
    return replacePlaceholder(msg, "{}", params.length, index -> {
      Object o = params[index];
      if (o == null) {
        return "null";
      } else if (o.getClass().isArray()) {
        String s = Arrays.deepToString(new Object[] {o});
        return s.substring(1, s.length());
      } else {
        return o.toString();
      }
    });
  }

  public static String replacePlaceholder(String messege, String placeholder, int numOfPlaceholder,
      Function<Integer, String> placeholderReplacer) {
    final int placeholderLength = placeholder.length();
    final StringBuilder sbuf = new StringBuilder(messege.length() + 50);
    int i = 0;
    int j;
    for (int p = 0; p < numOfPlaceholder; p++) {
      j = messege.indexOf(placeholder, i);
      sbuf.append(messege, i, j);
      sbuf.append(placeholderReplacer.apply(p));
      i = j + placeholderLength;
    }
    sbuf.append(messege, i, messege.length());
    return sbuf.toString();
  }
}
