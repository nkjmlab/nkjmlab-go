package org.nkjmlab.go.javalin.model.common;

import java.util.stream.Stream;

/**
 * stone<br>
 * 1桁目 0:ブランク， 1:黒， 2:白, 3:A, 4:B, 5:C<br>
 * 2桁目 0：ブランク， 1:□， 2：△, 3:x
 *
 */
public record Hand(String type, int number, int x, int y, int stone) {

  public static Hand createDummyHand() {
    return new Hand(HandType.DUMMY.getTypeName(), -1, -1, -1, -1);
  }

  /**
   * ProblemFactoryが使っているだけなので，fromAgehamaやvoteは不要かも
   *
   * @author nkjm
   *
   */
  public enum HandType {
    ON_BOARD("onBoard"), PUT_ON_BOARD("putOnBoard"), REMOVE_FROM_BOARD("removeFromBoard"), AGEHAMA(
        "agehama"), FROM_AGEHAMA("fromAgehama"), FROM_POD(
            "fromPod"), PASS("pass"), GIVE_UP("giveUp"), VOTE("vote"), DUMMY("dummy");

    private final String typeName;

    private HandType(String name) {
      this.typeName = name;
    }

    public String getTypeName() {
      return typeName;
    }

    public static HandType fromTypeName(String type) {
      return Stream.of(values()).filter(e -> e.getTypeName().equals(type)).findAny().get();
    }
  }

}
