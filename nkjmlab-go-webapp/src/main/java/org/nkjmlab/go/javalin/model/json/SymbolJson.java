package org.nkjmlab.go.javalin.model.json;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SymbolJson {

  private int x;
  private int y;
  private int symbol = 0;

  public SymbolJson() {}

  public SymbolJson(int x, int y, int symbol) {
    this.x = x;
    this.y = y;
    this.symbol = symbol;
  }

  public int getX() {
    return x;
  }

  public void setX(int x) {
    this.x = x;
  }

  public int getY() {
    return y;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getSymbol() {
    return symbol;
  }

  public void setSymbol(int stone) {
    this.symbol = stone;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
