package org.nkjmlab.util.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class ResourceUtils {

  public static String toResourceName(File file) {
    return toResourceName(file.getPath());
  }

  public static String toResourceName(String name) {
    return name.replaceAll("\\\\", "/");
  }

  public static BufferedReader getResourceAsBufferedReader(Class<?> clazz, String resourceName) {
    return new BufferedReader(getResourceAsInputStreamReader(clazz, resourceName));
  }

  public static InputStreamReader getResourceAsInputStreamReader(Class<?> clazz,
      String resourceName) {
    return new InputStreamReader(getResourceAsStream(clazz, resourceName));
  }

  public static InputStream getResourceAsStream(Class<?> clazz, String resourceName) {
    return clazz.getResourceAsStream(resourceName);
  }

  public static InputStream getResourceAsStream(Class<?> clazz, File file) {
    return getResourceAsStream(clazz, toResourceName(file));
  }

  public static InputStream getResourceAsStream(String resourceName) {
    return getResourceAsStream(ResourceUtils.class, resourceName);
  }

  public static File getFile(Class<?> clazz, String file) {
    return new File(getUri(clazz, file));
  }

  public static URI getUri(String file) {
    return getUri(ResourceUtils.class, file);
  }

  public static File getFile(String file) {
    return getFile(ResourceUtils.class, file);
  }

  public static URI getUri(Class<?> clazz, String file) {
    try {
      return clazz.getResource(file).toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> readAllLines(Class<?> clazz, String resourceName) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader br = getResourceAsBufferedReader(clazz, resourceName)) {
      String line;
      while ((line = br.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return lines;

  }

}
