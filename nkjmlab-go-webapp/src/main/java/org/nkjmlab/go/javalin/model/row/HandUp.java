package org.nkjmlab.go.javalin.model.row;

import java.io.Serializable;
import java.util.Date;
import org.nkjmlab.go.javalin.model.relation.HandsUpTable;
import org.nkjmlab.sorm4j.annotation.OrmTable;

@OrmTable(HandsUpTable.TABLE_NAME)
public class HandUp implements Serializable {

  private String gameId;
  private Date createdAt;
  private String message;

  public HandUp() {}

  private HandUp(String gameId) {
    this.gameId = gameId;
  }

  public HandUp(String gameId, Date createdAt, String message) {
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


  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
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
