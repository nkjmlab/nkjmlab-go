package org.nkjmlab.go.javalin.model.json;

import static org.nkjmlab.go.javalin.model.row.MatchingRequest.*;
import java.time.LocalDateTime;
import org.nkjmlab.go.javalin.model.row.MatchingRequest;

public class MatchingRequestJson {

  private String userId;
  private int rank;
  private LocalDateTime createdAt;
  private String seatId;
  private String userName;
  private String gameId = UNPAIRED;

  public MatchingRequestJson() {

  }

  public MatchingRequestJson(MatchingRequest request) {
    this.userId = request.getUserId();
    this.rank = request.getRank();
    this.createdAt = request.getCreatedAt();
    this.seatId = request.getSeatId();
    this.userName = request.getUserName();
    this.gameId = request.getGameId();

  }

  public boolean isUnpaired() {
    return UNPAIRED.equals(gameId);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getRank() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getSeatId() {
    return seatId;
  }

  public void setSeatId(String seatId) {
    this.seatId = seatId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

}
