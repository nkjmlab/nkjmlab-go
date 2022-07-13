package org.nkjmlab.go.javalin.model.common;

import org.nkjmlab.sorm4j.util.datatype.OrmJsonColumnContainer;

@OrmJsonColumnContainer
public record Agehama(int black, int white) {

  public Agehama increment(Stone stone) {
    if (stone.color() == Stone.Color.BLACK) {
      return new Agehama(black + 1, white);
    } else if (stone.color() == Stone.Color.WHITE) {
      return new Agehama(black, white + 1);
    } else {
      throw new IllegalArgumentException(stone + " is invalid");
    }
  }

}
