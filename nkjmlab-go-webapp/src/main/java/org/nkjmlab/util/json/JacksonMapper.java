package org.nkjmlab.util.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import org.apache.commons.lang3.ClassUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonMapper implements JsonMapper {
  private static final JacksonMapper defaultMapper =
      new JacksonMapper(JacksonUtils.getDefaultObjectMapper());

  private final ObjectMapper mapper;

  public static JacksonMapper getDefaultMapper() {
    return defaultMapper;
  }

  private JacksonMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public static JacksonMapper create(ObjectMapper mapper) {
    return new JacksonMapper(mapper);
  }


  public ObjectMapper getMapper() {
    return mapper;
  }

  @Override
  public <T> T toObject(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> T toObject(String json, TypeReference<T> toValueType) {
    try {
      return mapper.readValue(json, toValueType);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public <T> T toObject(File in, Class<T> clazz) {
    try {
      return mapper.readValue(in, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T toObject(Reader in, Class<T> clazz) {
    try {
      return mapper.readValue(in, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T toObject(InputStream in, Class<T> clazz) {
    try {
      return mapper.readValue(in, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toJson(Object obj) {
    return toJson(obj, false);
  }

  @Override
  public String toJson(Object obj, boolean prettyPrint) {
    try {
      if (prettyPrint) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
      } else {
        return mapper.writeValueAsString(obj);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void toJsonAndWrite(Object obj, File out, boolean prettyPrint) {
    try {
      if (prettyPrint) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(out, obj);
      } else {
        mapper.writeValue(out, obj);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void toJsonAndWrite(Object obj, Writer out, boolean prettyPrint) {
    try {
      if (prettyPrint) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(out, obj);
      } else {
        mapper.writeValue(out, obj);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void toJsonAndWrite(Object obj, OutputStream out, boolean prettyPrint) {
    try {
      if (prettyPrint) {
        mapper.writerWithDefaultPrettyPrinter().writeValue(out, obj);
      } else {
        mapper.writeValue(out, obj);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object toObject(String json, Object hint) {
    if (hint instanceof JavaType) {
      return toObject(json, (JavaType) hint);
    } else if (hint instanceof TypeReference) {
      return toObject(json, (TypeReference) hint);
    } else if (hint instanceof Class<?>) {
      return toObject(json, (Class<?>) hint);
    } else {
      throw new IllegalArgumentException("hint is invalid.");
    }
  }

  @Override
  public Object toObject(String json, Type hint) {
    try {
      if (hint instanceof Class<?>) {
        return toObject(json, (Class<?>) hint);
      } else {
        return mapper.readValue(json, mapper.getTypeFactory().constructType(hint));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void enable(DeserializationFeature feature) {
    mapper.enable(feature);
  }

  @Override
  public <T> T convertValue(Object fromValue, Class<T> toValueType) {
    if (fromValue == null) {
      return null;
    }
    if (ClassUtils.isAssignable(fromValue.getClass(), toValueType)) {
      return (T) fromValue;
    }
    return mapper.convertValue(fromValue, toValueType);
  }

  @Override
  public Type convertValue(Object fromValue, Type toValueType) {
    if (fromValue == null) {
      return null;
    }
    return mapper.convertValue(fromValue, mapper.getTypeFactory().constructType(toValueType));

  }

  public <T> T convertValue(Object fromValue, TypeReference<T> toValueType) {
    return mapper.convertValue(fromValue, toValueType);
  }


}
