package org.nkjmlab.go.javalin.model.relation;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.Agehama;
import org.nkjmlab.go.javalin.model.common.Hand;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.context.SormContext;
import org.nkjmlab.sorm4j.extension.datatype.jackson.JacksonSupport;
import org.nkjmlab.sorm4j.extension.h2.functions.system.CsvWrite;
import org.nkjmlab.sorm4j.extension.h2.orm.table.definition.H2DefinedTableBase;
import org.nkjmlab.sorm4j.sql.statement.SqlKeyword;
import org.nkjmlab.sorm4j.sql.statement.SqlTrait;
import org.nkjmlab.sorm4j.table.definition.annotation.AutoIncrement;
import org.nkjmlab.sorm4j.table.definition.annotation.Index;
import org.nkjmlab.sorm4j.table.definition.annotation.IndexColumnPair;
import org.nkjmlab.sorm4j.table.definition.annotation.NotNull;
import org.nkjmlab.sorm4j.table.definition.annotation.PrimaryKey;

public class GameStatesTable extends H2DefinedTableBase<GameState> implements SqlTrait, SqlKeyword {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

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

  public GameStatesTable(DataSource dataSource) {
    super(Sorm.create(dataSource, buildContext()), GameState.class);
  }

  private static SormContext buildContext() {
    org.nkjmlab.sorm4j.context.SormContext.Builder builder = SormContext.builder();
    new JacksonSupport(GoApplication.getDefaultJacksonMapper().getObjectMapper())
        .addSupport(builder);
    return builder.build();
  }

  Optional<GameState> getLatestGameStateFromDb(String gameId) {
    GameState gameState =
        readFirst(
            selectStarFrom(getTableName()) + where(GAME_ID + "=?") + orderByDesc(ID) + limit(1),
            gameId);
    return gameState == null ? Optional.empty() : Optional.of(gameState);
  }

  public Set<String> readPastOpponentsUserIds(String userId) {
    Set<String> set =
        new HashSet<>(
            getOrm()
                .readList(
                    String.class,
                    "SELECT DISTINCT * FROM (SELECT BLACK_PLAYER_ID FROM GAME_STATES WHERE WHITE_PLAYER_ID =? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE UNION SELECT WHITE_PLAYER_ID FROM GAME_STATES WHERE BLACK_PLAYER_ID = ? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE)",
                    userId,
                    userId));
    set.remove(userId);
    return set;
  }

  private static final String ROWNUM = " ROWNUM ";

  public void trimAndBackupToFile(File backUpDir, int limit) {
    Long maxRowNum =
        getOrm().readFirst(Long.class, select(func(MAX, ROWNUM)) + from(getTableName()));
    maxRowNum = maxRowNum == null ? 0 : maxRowNum;

    int deleteRowNum = (int) (maxRowNum - limit);

    if (deleteRowNum < 1) {
      return;
    }

    File outputFile = new File(backUpDir, getTableName() + System.currentTimeMillis() + ".csv");
    String selectSql =
        selectStarFrom(getTableName()) + where(cond(ROWNUM, "<=", deleteRowNum)) + orderBy(ID);

    String st = CsvWrite.builder(outputFile).query(selectSql).build().getSql();
    log.info("{}", st);
    getOrm().executeUpdate("CALL " + st);

    List<GameState> dels = readList(selectSql);
    delete(dels.toArray(GameState[]::new));

    log.info("trim and backup to {}.", outputFile);
  }

  public void delete(String gameId) {
    getLatestGameStateFromDb(gameId).ifPresent(state -> delete(state));
  }

  public List<String> readTodayGameIds() {
    return getOrm()
        .readList(
            String.class,
            "SELECT DISTINCT GAME_ID FROM GAME_STATES  WHERE GAME_ID LIKE '%-vs-%' AND CAST(CREATED_AT AS DATE) = CURRENT_DATE");
  }

  @IndexColumnPair({BLACK_PLAYER_ID, WHITE_PLAYER_ID})
  public record GameState(
      @PrimaryKey @AutoIncrement long id,
      LocalDateTime createdAt,
      @Index @NotNull String gameId,
      @NotNull String blackPlayerId,
      @NotNull String whitePlayerId,
      @NotNull Hand lastHand,
      @NotNull Agehama agehama,
      @NotNull Integer[][] cells,
      @NotNull Map<String, Integer> symbols,
      @NotNull Hand[] handHistory,
      @NotNull long problemId,
      @NotNull Map<String, Object> options) {

    public static final String DEFAULT_PLAYER_ID = "-1";
    public static final int DEFAULT_RO = 9;

    public GameState updateHandHistory(List<Hand> modifiedHistory) {
      return new GameState(
          id,
          createdAt,
          gameId,
          blackPlayerId,
          whitePlayerId,
          lastHand,
          agehama,
          cells,
          symbols,
          modifiedHistory.toArray(Hand[]::new),
          problemId,
          options);
    }
  }
}
