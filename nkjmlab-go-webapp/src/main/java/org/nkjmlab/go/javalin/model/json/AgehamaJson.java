package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.Stone;

public class AgehamaJson {

  private int black = 0;
  private int white = 0;

  public AgehamaJson() {

  }

  public int getBlack() {
    return black;
  }

  public void setBlack(int black) {
    this.black = black;
  }

  public int getWhite() {
    return white;
  }

  public void setWhite(int white) {
    this.white = white;
  }

  public void increment(Stone stone) {
    if (stone.getColor() == Stone.Color.BLACK) {
      black++;
    } else if (stone.getColor() == Stone.Color.WHITE) {
      white++;
    } else {
      throw new IllegalArgumentException(stone + " is invalid");
    }
  }

}
