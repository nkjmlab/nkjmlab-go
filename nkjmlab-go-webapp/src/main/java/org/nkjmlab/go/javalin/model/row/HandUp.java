package org.nkjmlab.go.javalin.model.row;

import java.io.Serializable;
import java.time.LocalDateTime;
import org.nkjmlab.go.javalin.model.relation.HandsUpTable;
import org.nkjmlab.sorm4j.annotation.OrmTable;

@OrmTable(HandsUpTable.TABLE_NAME)
public class HandUp implements Serializable {

  private String gameId;
  private LocalDateTime createdAt;
  private String message;

  public HandUp() {}

  private HandUp(String gameId) {
    this.gameId = gameId;
  }

  public HandUp(String gameId, LocalDateTime createdAt, String message) {
    this(gameId);
    this.createdAt = createdAt;
    this.message = message;
  }


  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }


  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public static HandUp createAsPrimaryKey(String gameId) {
    return new HandUp(gameId);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
