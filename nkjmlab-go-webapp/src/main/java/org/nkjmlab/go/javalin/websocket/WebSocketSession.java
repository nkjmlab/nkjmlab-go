package org.nkjmlab.go.javalin.websocket;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.nkjmlab.sorm4j.annotation.OrmTable;

@OrmTable(WebsoketSessionsTable.TABLE_NAME)
public class WebSocketSession {

  private int sessionId;
  private String gameId;
  private String userId;
  private Date createdAt = new Date();
  public int globalMessageCount = 0;

  public WebSocketSession() {}

  public WebSocketSession(int sessionId, String gameId, String userId) {
    this.sessionId = sessionId;
    this.gameId = gameId;
    this.userId = userId;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
  }

  public String getGameId() {
    return gameId;
  }

  public void setGameId(String gameId) {
    this.gameId = gameId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

}
