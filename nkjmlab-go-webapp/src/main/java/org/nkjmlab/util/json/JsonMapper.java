package org.nkjmlab.util.json;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public interface JsonMapper {

  Object toObject(String json, Object hint);

  Object toObject(String json, Type hint);

  <T> T toObject(String json, Class<T> clazz);

  <T> T toObject(File in, Class<T> clazz);

  <T> T toObject(Reader in, Class<T> clazz);

  <T> T toObject(InputStream in, Class<T> clazz);

  String toJson(Object obj);

  String toJson(Object obj, boolean prettyPrint);

  void toJsonAndWrite(Object obj, File out, boolean prettyPrint);

  void toJsonAndWrite(Object obj, Writer out, boolean prettyPrint);

  void toJsonAndWrite(Object obj, OutputStream out, boolean prettyPrint);

  <T> T convertValue(Object fromValue, Class<T> toValueType);

  Type convertValue(Object fromValue, Type toValueType);
}
