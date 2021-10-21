package org.nkjmlab.go.javalin.model.json;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class HandJson {

  private String type = "";
  private int number = -1;
  private int x = -1;
  private int y = -1;

  // 1桁目 0:ブランク， 1:黒， 2:白, 3:A, 4:B, 5:C
  // 2桁目 0：ブランク， 1:□， 2：△, 3:x
  private int stone = 0;
  private String options = "";

  public HandJson() {

  }

  public HandJson(HandType type, int number, int x, int y, int stone) {
    this.type = type.getTypeName();
    this.number = number;
    this.x = x;
    this.y = y;
    this.stone = stone;
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

  public int getStone() {
    return stone;
  }

  public void setStone(int color) {
    this.stone = color;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(String options) {
    this.options = options;
  }

}
