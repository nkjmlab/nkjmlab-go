package org.nkjmlab.go.javalin.model.common;

import org.nkjmlab.go.javalin.model.common.Stone.Color;
import org.nkjmlab.go.javalin.model.common.Stone.Symbol;

@SuppressWarnings("unused")

/**
 * id<br>
 * 1桁目 0:ブランク， 1:黒， 2:白, 3:A, 4:B, 5:C<br>
 * 2桁目 0：ブランク， 1:□， 2：△, 3:x
 */
public record Stone(int id, Color color, Symbol symbol) {

  public Stone(int id) {
    this(id, Color.of(id), Symbol.of(id));
  }

  public static enum Color {
    BLANK(0),
    BLACK(1),
    WHITE(2);

    private final int id;

    private Color(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public static Color of(int id) {
      int tmp = id % 10;
      for (Color c : values()) {
        if (c.getId() == tmp) {
          return c;
        }
      }
      throw new IllegalArgumentException(id + " is invalid.");
    }
  }

  public static enum Symbol {
    BLANK(0),
    A(40),
    B(50),
    C(60),
    RECTANGLE(10),
    TRIANGLE(20),
    X(30),
    CIRCLE(70);

    // TSUKADA BLANK(0), A(10), B(20), C(30), RECTANGLE(40), TRIANGLE(50), X(70);
    private final int id;

    private Symbol(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    public static Symbol of(int id) {
      int tmp = ((id / 10) % 10) * 10;
      for (Symbol s : values()) {
        if (s.getId() == tmp) {
          return s;
        }
      }
      throw new IllegalArgumentException(id + " is invalid.");
    }
  }
}
