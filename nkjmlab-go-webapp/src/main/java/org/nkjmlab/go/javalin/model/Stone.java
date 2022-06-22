package org.nkjmlab.go.javalin.model;

public class Stone {

  public static enum Color {
    BLANK(0), BLACK(1), WHITE(2);

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

    BLANK(0), A(40), B(50), C(60), RECTANGLE(10), TRIANGLE(20), X(30), CIRCLE(70);

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

  private int id;
  private Color color;
  private Symbol symbol;

  public Stone() {}

  public Stone(int id) {
    setId(id);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
    this.color = Color.of(id);
    symbol = Symbol.of(id);
  }

  public Color getColor() {
    return color;
  }

  public Symbol getSymbol() {
    return symbol;
  }

}
