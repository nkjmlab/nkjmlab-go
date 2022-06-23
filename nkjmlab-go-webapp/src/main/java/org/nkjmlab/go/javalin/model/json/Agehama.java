package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.Stone;

public record Agehama(int black, int white) {

  public Agehama increment(Stone stone) {
    if (stone.getColor() == Stone.Color.BLACK) {
      return new Agehama(black + 1, white);
    } else if (stone.getColor() == Stone.Color.WHITE) {
      return new Agehama(black, white + 1);
    } else {
      throw new IllegalArgumentException(stone + " is invalid");
    }
  }

}
