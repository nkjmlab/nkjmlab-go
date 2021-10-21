package org.nkjmlab.go.javalin.model.row;

import java.sql.Timestamp;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Login {

  private long id;
  private String userId;
  private String userName;
  private String seatId = "";
  private Timestamp loggedInAt;
  private String remoteAddr;

  public Login() {}

  public Login(String userId, String seatId, String userName, Timestamp loggedInAt,
      String remoteAddr) {
    this.userId = userId;
    this.seatId = seatId;
    this.userName = userName;
    this.loggedInAt = loggedInAt;
    this.remoteAddr = remoteAddr;
  }

  public Login(User u, Timestamp loggedInAt, String remoteAddr) {
    this(u.getUserId(), u.getSeatId(), u.getUserName(), loggedInAt, remoteAddr);
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String id) {
    this.userName = id;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public Timestamp getLoggedInAt() {
    return loggedInAt;
  }

  public void setLoggedInAt(Timestamp created) {
    this.loggedInAt = created;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String seatId) {
    this.userId = seatId;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getSeatId() {
    return seatId;
  }

  public void setSeatId(String seatId) {
    this.seatId = seatId;
  }

  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  public String getRemoteAddr() {
    return remoteAddr;
  }

}
