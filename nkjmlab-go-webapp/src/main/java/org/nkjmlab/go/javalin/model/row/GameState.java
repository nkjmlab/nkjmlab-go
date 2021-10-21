package org.nkjmlab.go.javalin.model.row;

import java.util.Date;

public class GameState {

  private long id;
  private Date createdAt;
  private String gameId;
  private String blackPlayerId;
  private String whitePlayerId;
  private String lastHand;
  private String agehama;
  private String cells;
  private String symbols;
  private String handHistory;
  private long problemId;
  private String options;

  public GameState() {}

  public GameState(String gameId) {
    this.gameId = gameId;
  }

  public GameState(Date createdAt, String gameId, String blackPlayerId, String whitePlayerId,
      int handNumber, String lastHand, String agehama, String cells, String symbols,
      String handHistory, long problemId, String options) {
    this.createdAt = createdAt;
    this.gameId = gameId;
    this.blackPlayerId = blackPlayerId;
    this.whitePlayerId = whitePlayerId;
    this.lastHand = lastHand;
    this.agehama = agehama;
    this.cells = cells;
    this.symbols = symbols;
    this.handHistory = handHistory;
    this.problemId = problemId;
    this.options = options;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getBlackPlayerId() {
    return blackPlayerId;
  }

  public void setBlackPlayerId(String blackPlayerId) {
    this.blackPlayerId = blackPlayerId;
  }

  public String getWhitePlayerId() {
    return whitePlayerId;
  }

  public void setWhitePlayerId(String whitePlayerId) {
    this.whitePlayerId = whitePlayerId;
  }

  public String getCells() {
    return cells;
  }

  public void setCells(String cells) {
    this.cells = cells;
  }

  public String getLastHand() {
    return lastHand;
  }

  public void setLastHand(String hand) {
    this.lastHand = hand;
  }

  public String getAgehama() {
    return agehama;
  }

  public void setAgehama(String agehama) {
    this.agehama = agehama;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getHandHistory() {
    return handHistory;
  }

  public void setHandHistory(String hands) {
    this.handHistory = hands;
  }

  public long getProblemId() {
    return problemId;
  }

  public void setProblemId(long problemId) {
    this.problemId = problemId;
  }

  public String getSymbols() {
    return symbols;
  }

  public void setSymbols(String symbols) {
    this.symbols = symbols;
  }

  public String getOptions() {
    return options;
  }

  public void setOptions(String option) {
    this.options = option;
  }

}
