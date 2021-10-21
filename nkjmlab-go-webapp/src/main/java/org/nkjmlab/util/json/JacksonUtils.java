package org.nkjmlab.util.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.nkjmlab.util.io.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonUtils {
  private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();

  public static ObjectMapper getDefaultObjectMapper() {
    return DEFAULT_OBJECT_MAPPER;
  }



  public static Map<String, Object> readJsonAsMap(File file) {
    try {
      Map<String, Object> map = DEFAULT_OBJECT_MAPPER.readValue(file,
          new TypeReference<LinkedHashMap<String, Object>>() {});
      return map;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(file + " is invalid.");
    }

  }

  public static Map<String, Object> readJsonAsMap(String json) {
    if (json.length() == 0) {
      return new LinkedHashMap<>();
    }
    try {
      Map<String, Object> map = DEFAULT_OBJECT_MAPPER.readValue(json,
          new TypeReference<LinkedHashMap<String, Object>>() {});
      return map;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(json + " is invalid.");
    }

  }

  public static <T> T readValue(String source, Class<T> clazz) {
    try {
      return DEFAULT_OBJECT_MAPPER.readValue(source, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T readValue(File file, Class<T> clazz) {
    return readValue(FileUtils.newBufferedReader(file.toPath()), clazz);
  }

  public static <T> T readValue(InputStream in, Class<T> clazz) {
    return readValue(new InputStreamReader(in), clazz);
  }

  public static <T> T readValue(Reader reader, Class<T> clazz) {
    try {
      return DEFAULT_OBJECT_MAPPER.readValue(reader, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String writeValueAsString(Object value) {
    try {
      return DEFAULT_OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeValue(File file, Object source) {
    try {
      DEFAULT_OBJECT_MAPPER.writeValue(file, source);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeValue(Writer writer, Object source) {
    try {
      DEFAULT_OBJECT_MAPPER.writeValue(writer, source);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeValue(OutputStream out, Object source) {
    try {
      DEFAULT_OBJECT_MAPPER.writeValue(out, source);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }



}
