package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.Stone;

public record AgehamaJson(int black, int white) {

  public AgehamaJson increment(Stone stone) {
    if (stone.getColor() == Stone.Color.BLACK) {
      return new AgehamaJson(black + 1, white);
    } else if (stone.getColor() == Stone.Color.WHITE) {
      return new AgehamaJson(black, white + 1);
    } else {
      throw new IllegalArgumentException(stone + " is invalid");
    }
  }



}
