package org.nkjmlab.go.javalin.model.json;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nkjmlab.go.javalin.model.row.GameState;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.json.JsonMapper;

public class GameStateJson {

  public static final String DEFAULT_PLAYER_ID = "-1";
  public static final int DEFAULT_RO = 9;

  private long id;
  private String gameId;
  private String blackPlayerId = DEFAULT_PLAYER_ID;
  private String whitePlayerId = DEFAULT_PLAYER_ID;
  private int[][] cells;
  private Map<String, Integer> symbols = new HashMap<>();
  private AgehamaJson agehama;
  private HandJson lastHand = new HandJson();
  private List<HandJson> handHistory = new ArrayList<>();
  private long problemId = -1;
  private Map<String, Object> options = new HashMap<>();
  private LocalDateTime createdAt = LocalDateTime.now();

  private static final JsonMapper mapper = JacksonMapper.getDefaultMapper();

  public GameStateJson() {

  }

  public GameStateJson(String gameId, String blackPlayerId, String whitePlayerId, int[][] cells,
      Map<String, Integer> symbols, AgehamaJson agehama, HandJson lastHand,
      List<HandJson> handHistory, long problemId, Map<String, Object> options) {
    this.gameId = gameId;
    this.blackPlayerId = blackPlayerId;
    this.whitePlayerId = whitePlayerId;
    this.cells = cells;
    this.symbols = symbols;
    this.agehama = agehama;
    this.lastHand = lastHand;
    this.handHistory = handHistory;
    this.problemId = problemId;
    this.options = options;
  }

  public GameStateJson(GameState gameState) {
    this.setId(gameState.getId());
    this.gameId = gameState.getGameId();
    this.blackPlayerId = gameState.getBlackPlayerId();
    this.whitePlayerId = gameState.getWhitePlayerId();
    this.cells = GameStateUtils.cellsStringToCellsArray(gameState.getCells());
    this.symbols = GameStateUtils.symbolsStringToSymbols(gameState.getSymbols());
    this.lastHand = mapper.toObject(gameState.getLastHand(), HandJson.class);
    this.agehama = mapper.toObject(gameState.getAgehama(), AgehamaJson.class);
    this.handHistory = Arrays.asList(mapper.toObject(gameState.getHandHistory(), HandJson[].class));
    this.problemId = gameState.getProblemId();
    this.options = GameStateUtils.optionsStringToMap(gameState.getOptions());
    this.createdAt = gameState.getCreatedAt();
  }

  public GameState toGameState() {
    return new GameState(LocalDateTime.now(), gameId, blackPlayerId, whitePlayerId,
        lastHand.getNumber(), mapper.toJson(lastHand), mapper.toJson(agehama), mapper.toJson(cells),
        mapper.toJson(symbols), mapper.toJson(handHistory), problemId, mapper.toJson(options));
  }

  public int[][] getCells() {
    return cells;
  }

  public void setCells(int[][] cells) {
    this.cells = cells;
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

  public HandJson getLastHand() {
    return lastHand;
  }

  public void setLastHand(HandJson hand) {
    this.lastHand = hand;
  }

  public AgehamaJson getAgehama() {
    return agehama;
  }

  public void setAgehama(AgehamaJson agehama) {
    this.agehama = agehama;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public List<HandJson> getHandHistory() {
    return handHistory;
  }

  public void setHandHistory(List<HandJson> handHistory) {
    this.handHistory = handHistory;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getProblemId() {
    return problemId;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public void setOptions(Map<String, Object> options) {
    this.options = options;
  }


  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }


  public Map<String, Integer> getSymbols() {
    return symbols;
  }

  public void setSymbols(Map<String, Integer> symbols) {
    this.symbols = symbols;
  }

  public void setProblemId(long problemId) {
    this.problemId = problemId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }


}
