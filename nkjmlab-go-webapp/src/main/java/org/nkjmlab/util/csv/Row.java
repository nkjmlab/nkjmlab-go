package org.nkjmlab.util.csv;

import java.util.Arrays;
import java.util.List;

public final class Row {

  private final List<String> cells;

  public Row(String[] line) {
    this.cells = Arrays.asList(line);
  }

  public Row(List<String> line) {
    this.cells = line;
  }


  public String get(int index) {
    try {
      return cells.get(index).trim();
    } catch (Exception e) {
      return null;
    }
  }

  public Integer getAsInteger(int index) {
    try {
      return Integer.valueOf(get(index));
    } catch (Exception e) {
      return null;
    }
  }

  public Double getAsDouble(int index) {
    try {
      return Double.valueOf(get(index));
    } catch (Exception e) {
      return null;
    }
  }

  public boolean isHeadCellEquals(String str) {
    return get(0).equals(str);
  }

  public Boolean getAsBoolean(int index) {
    try {
      return Boolean.valueOf(get(index));
    } catch (Exception e) {
      return null;
    }
  }

  public Float getAsFloat(int index) {
    try {
      return Float.valueOf(get(index));
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public String toString() {
    return "Row [cells=" + cells + "]";
  }


}
