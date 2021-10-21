package org.nkjmlab.go.javalin.model.json;

/**
 * ProblemFactoryが使っているだけなので，fromAgehamaやvoteは不要かも
 *
 * @author nkjm
 *
 */
public enum HandType {
  ON_BOARD("onBoard"), PUT_ON_BOARD("putOnBoard"), REMOVE_FROM_BOARD("removeFromBoard"), AGEHAMA(
      "agehama"), FROM_AGEHAMA(
          "fromAgehama"), FROM_POD("fromPod"), PASS("pass"), GIVE_UP("giveUp"), VOTE("vote");

  private final String typeName;

  private HandType(String name) {
    this.typeName = name;
  }

  public String getTypeName() {
    return typeName;
  }
}
