package org.nkjmlab.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class IOStreamUtils {
  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static String readAsString(InputStream is, Charset charset) throws IOException {
    InputStreamReader reader = new InputStreamReader(is, charset);
    return readAsString(reader);
  }

  public static String readAsString(Reader reader) throws IOException {
    final char[] buffer = new char[1024];
    final StringBuilder sb = new StringBuilder();

    while (true) {
      int size = reader.read(buffer);
      if (size == -1)
        break;
      sb.append(buffer, 0, size);
    }
    return sb.toString();
  }

  public static String toUtf8(String str) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream();
        Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
      w.write(str);
      w.flush();
      String results = os.toString(StandardCharsets.UTF_8);
      return results;
    } catch (Exception e) {
      log.error(e);
      throw new RuntimeException(e);
    }

  }
}
