package org.nkjmlab.go.javalin.model.row;

import java.time.LocalDateTime;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Problem {

  private long id;
  private LocalDateTime createdAt;
  private String name = "";
  private String groupId = "";
  private String cells = "";
  private String symbols = "";
  private String message = "";
  private String handHistory = "";
  private String agehama = "";

  public Problem() {

  }

  public Problem(long id) {
    this.id = id;
    this.createdAt = LocalDateTime.now();

  }

  public Problem(long id, LocalDateTime createdAt, String groupId, String name, String cells,
      String symbols, String message, String handHistory, String agehama) {
    this.id = id;
    this.createdAt = createdAt;
    this.groupId = groupId;
    this.name = name;
    this.cells = cells;
    this.symbols = symbols;
    this.message = message;
    this.handHistory = handHistory;
    this.agehama = agehama;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getCells() {
    return cells;
  }

  public void setCells(String cells) {
    this.cells = cells;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getHandHistory() {
    return handHistory;
  }

  public void setHandHistory(String handHistory) {
    this.handHistory = handHistory;
  }

  public String getAgehama() {
    return agehama;
  }

  public void setAgehama(String agehama) {
    this.agehama = agehama;
  }

  public String getSymbols() {
    return symbols;
  }

  public void setSymbols(String symbols) {
    this.symbols = symbols;
  }

}
