package org.nkjmlab.go.javalin.model.relation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.json.AgehamaJson;
import org.nkjmlab.go.javalin.model.json.GameStateJson;
import org.nkjmlab.go.javalin.model.json.HandJson;
import org.nkjmlab.go.javalin.model.row.GameState;
import org.nkjmlab.sorm4j.internal.util.Try;

public class GameStatesTables {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static final String VS_SEPARATOR = "-vs-";

  private GameStatesTable gameStatesTableInMem;
  private GameStatesTable gameStatesTableInFile;
  private ExecutorService fileDbService = Executors.newSingleThreadExecutor();

  public GameStatesTables(DataSource fileDb, DataSource memDb) {
    this.gameStatesTableInMem = new GameStatesTable(memDb);
    this.gameStatesTableInFile = new GameStatesTable(fileDb);
  }

  private static final Map<String, GameStateJson> statesCache = new ConcurrentHashMap<>();


  public void saveGameState(GameStateJson json) {
    statesCache.put(json.getGameId(), json);
    GameState gs = json.toGameState();
    gameStatesTableInMem.insert(gs);
    fileDbService.execute(
        Try.createRunnable(() -> gameStatesTableInFile.insert(gs), e -> log.error(e.getMessage())));
  }

  public GameStateJson readLatestGameStateJson(String gameId) {
    return statesCache.computeIfAbsent(gameId,
        key -> gameStatesTableInMem.getLatestGameStateFromDb(gameId).map(s -> new GameStateJson(s))
            .orElseGet(() -> createNewGameState(gameId, GameStateJson.DEFAULT_PLAYER_ID,
                GameStateJson.DEFAULT_PLAYER_ID, GameStateJson.DEFAULT_RO)));
  }

  public List<GameStateJson> readLatestBoardsJson(List<String> gids) {
    return gids.stream().map(gid -> readLatestGameStateJson(gid)).collect(Collectors.toList());
  }

  public List<GameStateJson> readTodayGameJsons() {
    return readLatestBoardsJson(gameStatesTableInMem.readTodayGameIds());
  }


  public void deleteLatestGameState(String gameId) {
    gameStatesTableInMem.delete(gameId);
    statesCache.put(gameId,
        gameStatesTableInMem.getLatestGameStateFromDb(gameId).map(s -> new GameStateJson(s))
            .orElseGet(() -> createNewGameState(gameId, GameStateJson.DEFAULT_PLAYER_ID,
                GameStateJson.DEFAULT_PLAYER_ID, GameStateJson.DEFAULT_RO)));
  }



  public GameStateJson createNewGameState(String gameId, String blackPlayerId, String whitePlayerId,
      int ro) {
    String[] players = {blackPlayerId, whitePlayerId};
    if (gameId.split(VS_SEPARATOR).length == 2) {
      players = gameId.split(VS_SEPARATOR);
    }
    int[][] cells = new int[ro][ro];
    for (int i = 0; i < cells.length; i++) {
      Arrays.fill(cells[i], 0);
    }
    return new GameStateJson(gameId, players[0], players[1], cells, new HashMap<>(),
        new AgehamaJson(0, 0), new HandJson(), new ArrayList<>(), -1, new HashMap<>());
  }



  public Set<String> readPastOpponents(String userId) {
    return gameStatesTableInMem.readPastOpponentsUserIds(userId);
  }



}
