package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.h2.sql.H2CsvFunctions.*;
import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.row.GameState;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;

public class GameStatesTable {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


  public static final String TABLE_NAME = "GAME_STATES";

  public static final String ID = "id";
  public static final String CREATED_AT = "created_at";
  public static final String GAME_ID = "game_id";
  public static final String BLACK_PLAYER_ID = "black_player_id";
  public static final String WHITE_PLAYER_ID = "white_player_id";
  public static final String LAST_HAND = "last_hand";
  public static final String AGEHAMA = "agehama";
  public static final String CELLS = "cells";
  public static final String SYMBOLS = "symbols";
  public static final String HAND_HISTORY = "hand_history";
  public static final String PROBLEM_ID = "problem_id";
  public static final String OPTIONS = "options";


  private Sorm sorm;
  private TableDefinition schema;

  public GameStatesTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableDefinition.builder(TABLE_NAME)
        .addColumnDefinition(ID, BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).addColumnDefinition(GAME_ID, VARCHAR, NOT_NULL)
        .addColumnDefinition(BLACK_PLAYER_ID, VARCHAR, NOT_NULL)
        .addColumnDefinition(WHITE_PLAYER_ID, VARCHAR, NOT_NULL)
        .addColumnDefinition(LAST_HAND, VARCHAR, NOT_NULL)
        .addColumnDefinition(AGEHAMA, VARCHAR, NOT_NULL)
        .addColumnDefinition(CELLS, VARCHAR, NOT_NULL)
        .addColumnDefinition(SYMBOLS, VARCHAR, NOT_NULL)
        .addColumnDefinition(HAND_HISTORY, VARCHAR, NOT_NULL)
        .addColumnDefinition(PROBLEM_ID, BIGINT, NOT_NULL)
        .addColumnDefinition(OPTIONS, VARCHAR, NOT_NULL).addIndexDefinition(GAME_ID)
        .addIndexDefinition(BLACK_PLAYER_ID, WHITE_PLAYER_ID).build();
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);

  }


  Optional<GameState> getLatestGameStateFromDb(String gameId) {
    GameState gameState = sorm.readFirst(GameState.class,
        selectStarFrom(TABLE_NAME) + where(GAME_ID + "=?") + orderByDesc(ID) + limit(1), gameId);
    return gameState == null ? Optional.empty() : Optional.of(gameState);
  }


  public Set<String> readPastOpponentsUserIds(String userId) {
    Set<String> set = new HashSet<>(sorm.readList(String.class,
        "SELECT DISTINCT * FROM (SELECT BLACK_PLAYER_ID FROM GAME_STATES WHERE WHITE_PLAYER_ID =? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE UNION SELECT WHITE_PLAYER_ID FROM GAME_STATES WHERE BLACK_PLAYER_ID = ? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE)",
        userId, userId));
    set.remove(userId);
    return set;
  }

  private static final String ROWNUM = " ROWNUM ";

  public void trimAndBackupToFile(File backUpDir, int limit) {
    Long maxRowNum = sorm.readFirst(Long.class, select(func(MAX, ROWNUM)) + from(TABLE_NAME));
    maxRowNum = maxRowNum == null ? 0 : maxRowNum;

    int deleteRowNum = (int) (maxRowNum - limit);

    if (deleteRowNum < 1) {
      return;
    }

    File outputFile = new File(backUpDir, TABLE_NAME + System.currentTimeMillis() + ".csv");
    String selectSql =
        selectStarFrom(TABLE_NAME) + where(cond(ROWNUM, "<=", deleteRowNum)) + orderBy(ID);

    String st = getCallCsvWriteSql(outputFile, selectSql, StandardCharsets.UTF_8, ",");
    log.info("{}", st);
    sorm.executeUpdate(st);

    List<GameState> dels = sorm.readList(GameState.class, selectSql);
    sorm.delete(dels.toArray(GameState[]::new));

    log.info("trim and backup to {}.", outputFile);

  }


  public List<GameState> readAll() {
    return sorm.selectAll(GameState.class);
  }

  public void insert(GameState object) {
    sorm.insert(object);
  }

  public void insert(GameState... objects) {
    sorm.insert(objects);
  }


  public void delete(String gameId) {
    getLatestGameStateFromDb(gameId).ifPresent(state -> delete(state));
  }


  private void delete(GameState state) {
    sorm.delete(state);
  }


  public List<String> readTodayGameIds() {
    return sorm.readList(String.class,
        "SELECT DISTINCT GAME_ID FROM GAME_STATES  WHERE GAME_ID LIKE '%-vs-%' AND CAST(CREATED_AT AS DATE) = CURRENT_DATE");
  }


}
