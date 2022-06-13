package org.nkjmlab.go.javalin.model.row;

import java.time.LocalDateTime;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class MatchingRequest {

  public static final String UNPAIRED = "UNPAIRED";
  private String userId;
  private String userName;
  private String seatId;
  private int rank;
  private LocalDateTime createdAt;
  private String gameId = "";

  public MatchingRequest() {

  }

  public MatchingRequest(User u, LocalDateTime createdAt) {
    this.userId = u.getUserId();
    this.createdAt = createdAt;
    this.userName = u.getUserName();
    this.seatId = u.getSeatId();
    this.rank = u.getRank();
    this.gameId = UNPAIRED;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public MatchingRequest(String userId) {
    this.userId = userId;
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

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getSeatId() {
    return seatId;
  }

  public void setSeatId(String seatId) {
    this.seatId = seatId;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public boolean isUnpaired() {
    return UNPAIRED.equals(gameId);
  }

}
