package org.nkjmlab.go.javalin.model.row;

import java.time.LocalDateTime;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.sorm4j.annotation.OrmTable;

@OrmTable(GameRecordsTable.TABLE_NAME)
public class GameRecord {

  private int id;
  private LocalDateTime createdAt = LocalDateTime.now();
  private String userId;
  private String opponentUserId;
  private String jadge;
  private String memo;
  private int rank;
  private int point;
  private String message;

  public GameRecord() {}


  public GameRecord(String userId, String opponentUserId, String jadge, String memo, int rank,
      int point, String message) {
    this.userId = userId;
    this.opponentUserId = opponentUserId;
    this.jadge = jadge;
    this.memo = memo;
    this.rank = rank;
    this.point = point;
    this.message = message;
  }


  public String getOpponentUserId() {
    return opponentUserId;
  }


  public int getId() {
    return id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public String getUserId() {
    return userId;
  }

  public String getJadge() {
    return jadge;
  }

  public String getMemo() {
    return memo;
  }

  public int getRank() {
    return rank;
  }

  public int getPoint() {
    return point;
  }

  public String getMessage() {
    return message;
  }


  public void setId(int id) {
    this.id = id;
  }


  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }


  public void setUserId(String userId) {
    this.userId = userId;
  }


  public void setOpponentUserId(String opponentUserId) {
    this.opponentUserId = opponentUserId;
  }


  public void setJadge(String jadge) {
    this.jadge = jadge;
  }


  public void setMemo(String memo) {
    this.memo = memo;
  }


  public void setRank(int rank) {
    this.rank = rank;
  }


  public void setPoint(int point) {
    this.point = point;
  }


  public void setMessage(String message) {
    this.message = message;
  }

}
