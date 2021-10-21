package org.nkjmlab.go.javalin.model.json;

import org.nkjmlab.go.javalin.model.row.GameState;

public class PlayGameStateJson {

  private GameState gameState;

  public PlayGameStateJson(GameState gameState) {
    this.gameState = gameState;
  }

  public GameState getGameState() {
    return gameState;
  }

  public void setGameState(GameState gameState) {
    this.gameState = gameState;
  }

}
