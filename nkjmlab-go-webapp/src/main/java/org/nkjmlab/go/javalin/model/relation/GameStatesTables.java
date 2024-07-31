package org.nkjmlab.go.javalin.model.relation;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.nkjmlab.go.javalin.model.common.Agehama;
import org.nkjmlab.go.javalin.model.common.Hand;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.sorm4j.internal.util.Try;

public class GameStatesTables {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static final String VS_SEPARATOR = "-vs-";

  private final GameStatesTable gameStatesTableInMem;
  private final GameStatesTable gameStatesTableInFile;
  private final ExecutorService fileDbService = Executors.newSingleThreadExecutor();

  public GameStatesTables(GameStatesTable fileDb, GameStatesTable memDb) {
    this.gameStatesTableInMem = memDb;
    this.gameStatesTableInFile = fileDb;
  }

  private static final Map<String, GameState> statesCache = new ConcurrentHashMap<>();

  public void saveGameState(GameState json) {
    statesCache.put(json.gameId(), json);
    gameStatesTableInMem.insert(json);
    fileDbService.execute(
        Try.createRunnable(
            () -> gameStatesTableInFile.insert(json), e -> log.error(e.getMessage())));
  }

  public GameState readLatestGameState(String gameId) {
    return statesCache.computeIfAbsent(
        gameId,
        key ->
            gameStatesTableInMem
                .getLatestGameStateFromDb(gameId)
                .orElseGet(
                    () ->
                        createNewGameState(
                            gameId,
                            GameState.DEFAULT_PLAYER_ID,
                            GameState.DEFAULT_PLAYER_ID,
                            GameState.DEFAULT_RO)));
  }

  public List<GameState> readLatestBoardsJson(List<String> gids) {
    return gids.stream().map(gid -> readLatestGameState(gid)).collect(Collectors.toList());
  }

  public List<GameState> readTodayGameJsons() {
    return readLatestBoardsJson(gameStatesTableInMem.readTodayGameIds());
  }

  public void deleteLatestGameState(String gameId) {
    gameStatesTableInMem.delete(gameId);
    statesCache.put(
        gameId,
        gameStatesTableInMem
            .getLatestGameStateFromDb(gameId)
            .orElseGet(
                () ->
                    createNewGameState(
                        gameId,
                        GameState.DEFAULT_PLAYER_ID,
                        GameState.DEFAULT_PLAYER_ID,
                        GameState.DEFAULT_RO)));
  }

  public GameState createNewGameState(
      String gameId, String blackPlayerId, String whitePlayerId, int ro) {
    String[] players = {blackPlayerId, whitePlayerId};
    if (gameId.split(VS_SEPARATOR).length == 2) {
      players = gameId.split(VS_SEPARATOR);
    }
    Integer[][] cells = new Integer[ro][ro];
    for (int i = 0; i < cells.length; i++) {
      Arrays.fill(cells[i], 0);
    }

    return new GameState(
        -1,
        LocalDateTime.now(),
        gameId,
        players[0],
        players[1],
        Hand.createDummyHand(),
        new Agehama(0, 0),
        cells,
        new HashMap<>(),
        new Hand[0],
        -1,
        new HashMap<>());
  }

  public Set<String> readPastOpponents(String userId) {
    return gameStatesTableInMem.readPastOpponentsUserIds(userId);
  }
}
