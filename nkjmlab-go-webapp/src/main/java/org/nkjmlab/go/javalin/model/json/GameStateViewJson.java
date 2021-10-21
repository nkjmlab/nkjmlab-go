package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.row.HandUp;

public class GameStateViewJson {

  private GameStateJson gameState;
  private HandUp handUp;
  private int watchingStudentsNum = 0;

  public GameStateViewJson() {

  }

  public GameStateViewJson(GameStateJson gameState) {
    this.gameState = gameState;
  }


  public GameStateJson getGameState() {
    return gameState;
  }


  public void setGameState(GameStateJson gameState) {
    this.gameState = gameState;
  }


  public HandUp getHandUp() {
    return handUp;
  }

  public void setHandUp(HandUp handUp) {
    this.handUp = handUp;
  }

  public void setWatchingStudentsNum(int count) {
    this.watchingStudentsNum = count;
  }

  public int getWatchingStudentsNum() {
    return watchingStudentsNum;
  }


}
