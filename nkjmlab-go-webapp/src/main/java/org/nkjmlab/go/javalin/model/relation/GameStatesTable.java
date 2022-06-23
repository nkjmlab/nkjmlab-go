package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.h2.sql.H2CsvFunctions.*;
import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.AutoIncrement;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.IndexColumns;
import org.nkjmlab.sorm4j.util.table_def.annotation.NotNull;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.util.jackson.JacksonMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class GameStatesTable extends BasicH2Table<GameState> {
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


  public GameStatesTable(DataSource dataSource) {
    super(Sorm.create(dataSource), GameState.class);
    createTableIfNotExists().createIndexesIfNotExists();

  }


  Optional<GameState> getLatestGameStateFromDb(String gameId) {
    GameState gameState = readFirst(
        selectStarFrom(TABLE_NAME) + where(GAME_ID + "=?") + orderByDesc(ID) + limit(1), gameId);
    return gameState == null ? Optional.empty() : Optional.of(gameState);
  }


  public Set<String> readPastOpponentsUserIds(String userId) {
    Set<String> set = new HashSet<>(getOrm().readList(String.class,
        "SELECT DISTINCT * FROM (SELECT BLACK_PLAYER_ID FROM GAME_STATES WHERE WHITE_PLAYER_ID =? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE UNION SELECT WHITE_PLAYER_ID FROM GAME_STATES WHERE BLACK_PLAYER_ID = ? AND CAST(CREATED_AT AS DATE) = CURRENT_DATE)",
        userId, userId));
    set.remove(userId);
    return set;
  }

  private static final String ROWNUM = " ROWNUM ";

  public void trimAndBackupToFile(File backUpDir, int limit) {
    Long maxRowNum = getOrm().readFirst(Long.class, select(func(MAX, ROWNUM)) + from(TABLE_NAME));
    maxRowNum = maxRowNum == null ? 0 : maxRowNum;

    int deleteRowNum = (int) (maxRowNum - limit);

    if (deleteRowNum < 1) {
      return;
    }

    File outputFile = new File(backUpDir, TABLE_NAME + System.currentTimeMillis() + ".csv");
    String selectSql =
        selectStarFrom(TABLE_NAME) + where(cond(ROWNUM, "<=", deleteRowNum)) + orderBy(ID);

    String st = getCallCsvWriteSql(outputFile, selectSql, StandardCharsets.UTF_8, ',');
    log.info("{}", st);
    getOrm().executeUpdate(st);

    List<GameState> dels = readList(selectSql);
    delete(dels.toArray(GameState[]::new));

    log.info("trim and backup to {}.", outputFile);

  }

  public void delete(String gameId) {
    getLatestGameStateFromDb(gameId).ifPresent(state -> delete(state));
  }

  public List<String> readTodayGameIds() {
    return getOrm().readList(String.class,
        "SELECT DISTINCT GAME_ID FROM GAME_STATES  WHERE GAME_ID LIKE '%-vs-%' AND CAST(CREATED_AT AS DATE) = CURRENT_DATE");
  }


  @OrmRecord
  @IndexColumns({BLACK_PLAYER_ID, WHITE_PLAYER_ID})
  public record GameState(@PrimaryKey @AutoIncrement long id, LocalDateTime createdAt,
      @Index @NotNull String gameId, @NotNull String blackPlayerId, @NotNull String whitePlayerId,
      @NotNull String lastHand, @NotNull String agehama, @NotNull String cells,
      @NotNull String symbols, @NotNull String handHistory, @NotNull long problemId,
      @NotNull String options) {
  }

  public record GameStateJson(long id, String gameId, String blackPlayerId, String whitePlayerId,
      int[][] cells, Map<String, Integer> symbols, Agehama agehama, Hand lastHand,
      Hand[] handHistory, long problemId, Map<String, Object> options, LocalDateTime createdAt) {

    public static final String DEFAULT_PLAYER_ID = "-1";
    public static final int DEFAULT_RO = 9;

    private static final JacksonMapper mapper = GoApplication.getDefaultJacksonMapper();

    public GameStateJson updateHandHistory(List<Hand> modifiedHistory) {
      return new GameStateJson(id, gameId, blackPlayerId, whitePlayerId, cells, symbols, agehama,
          lastHand, modifiedHistory.toArray(Hand[]::new), problemId, options, createdAt);
    }


    public GameStateJson(GameState gameState) {
      this(gameState.id(), gameState.gameId(), gameState.blackPlayerId(), gameState.whitePlayerId(),
          mapper.toObject(gameState.cells(), int[][].class),
          mapper.toObject(gameState.symbols(), new TypeReference<Map<String, Integer>>() {}),
          mapper.toObject(gameState.agehama(), Agehama.class),
          mapper.toObject(gameState.lastHand(), Hand.class),
          mapper.toObject(gameState.handHistory(), Hand[].class), gameState.problemId(),
          mapper.toObject(gameState.options(), new TypeReference<Map<String, Object>>() {}),
          gameState.createdAt());
    }


    public GameState toGameState() {
      return new GameState(id(), LocalDateTime.now(), gameId, blackPlayerId, whitePlayerId,
          mapper.toJson(lastHand), mapper.toJson(agehama), mapper.toJson(cells),
          mapper.toJson(symbols), mapper.toJson(handHistory), problemId, mapper.toJson(options));
    }



  }

}
