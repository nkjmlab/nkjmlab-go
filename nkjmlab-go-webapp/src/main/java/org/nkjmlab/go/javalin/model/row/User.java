package org.nkjmlab.go.javalin.model.row;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.sorm4j.annotation.OrmTable;

/**
 * The User data is created by system at first. User can modified username, seat id and rank.
 */

@OrmTable(UsersTable.TABLE_NAME)
public class User {

  public static final String ADMIN = "ADMIN";
  public static final String STUDENT = "STUDENT";
  public static final String TA = "TA";
  public static final String GUEST = "GUEST";

  private String userId;
  private String email;
  private String userName;
  private String seatId;
  private String role = GUEST;
  private int rank;
  private Date createdAt;

  public User() {}

  public User(String userId) {
    this.userId = userId;
  }

  public User(String userId, String email, String userName, String role, String seatId, int rank,
      Date createdAt) {
    this(userId);
    this.email = email;
    this.userName = userName;
    this.role = role;
    this.seatId = seatId;
    this.rank = rank;
    this.createdAt = createdAt;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String name) {
    this.userName = name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date created) {
    this.createdAt = created;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String seatId) {
    this.userId = seatId;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }


  public boolean isAdmin() {
    return role != null && role.equalsIgnoreCase(ADMIN);
  }


  public boolean isStudent() {
    return role != null && role.equalsIgnoreCase(STUDENT);
  }

  public boolean isGuest() {
    return role != null && role.equalsIgnoreCase(GUEST);
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

}
