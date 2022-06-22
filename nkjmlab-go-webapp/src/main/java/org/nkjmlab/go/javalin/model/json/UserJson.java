package org.nkjmlab.go.javalin.model.json;

import java.time.LocalDateTime;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;

public class UserJson {

  private String userId;
  private String userName;
  private String seatId;
  private int rank;
  private LocalDateTime createdAt;
  public boolean attendance;

  public UserJson() {}

  public UserJson(User user) {
    this.userId = user.userId();
    this.seatId = user.seatId();
    this.userName = user.userName();
    this.createdAt = user.createdAt();
    this.rank = user.rank();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime modifiedAt) {
    this.createdAt = modifiedAt;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String nickname) {
    this.userName = nickname;
  }

  public String getSeatId() {
    return seatId;
  }

  public void setSeatId(String seatId) {
    this.seatId = seatId;
  }

  public int getRank() {
    return rank;
  }

  public void setRank(int rank) {
    this.rank = rank;
  }


}
