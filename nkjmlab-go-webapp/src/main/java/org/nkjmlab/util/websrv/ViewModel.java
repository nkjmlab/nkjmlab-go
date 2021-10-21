package org.nkjmlab.util.websrv;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

public class ViewModel implements Map<String, Object> {

  private static final String LOCALE = "LOCALE";
  private Map<String, Object> map = new LinkedHashMap<>();

  public ViewModel() {}

  public Map<String, Object> getMap() {
    return map;
  }

  @Override
  public String toString() {
    return map.toString();
  }

  public Locale getLocale() {
    return (Locale) map.get(LOCALE);
  }


  public static class Builder {

    private Map<String, Object> fileModifiedDate;

    private Locale locale = Locale.getDefault();

    public Builder setFileModifiedDate(File directory, boolean recursive, String... extentions) {
      Collection<File> files = FileUtils.listFiles(directory, extentions, recursive);
      this.fileModifiedDate = files.stream()
          .collect(Collectors.toMap(
              f -> f.getAbsolutePath().replace(directory.getAbsolutePath(), "").replace(".", "_")
                  .replace("-", "_").replace(File.separator, "_").replaceFirst("_", ""),
              f -> f.lastModified()));
      return this;
    }

    public Builder setLocale(Locale locale) {
      this.locale = locale;
      return this;
    }

    public ViewModel build() {
      ViewModel model = new ViewModel();
      model.putAll(fileModifiedDate);
      model.put(LOCALE, locale);
      return model;
    }

  }



  @Override
  public int size() {
    return map.size();
  }



  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }



  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }



  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }



  @Override
  public Object get(Object key) {
    return map.get(key);
  }



  @Override
  public Object put(String key, Object value) {
    return map.put(key, value);
  }



  @Override
  public Object remove(Object key) {
    return map.remove(key);
  }



  @Override
  public void putAll(Map<? extends String, ? extends Object> m) {
    map.putAll(m);
  }



  @Override
  public void clear() {
    map.clear();
  }



  @Override
  public Set<String> keySet() {
    return map.keySet();
  }



  @Override
  public Collection<Object> values() {
    return map.values();
  }



  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }



}
