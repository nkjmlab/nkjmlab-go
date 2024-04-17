package org.nkjmlab.go.javalin.websocket;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.Agehama;
import org.nkjmlab.go.javalin.model.common.Hand;
import org.nkjmlab.go.javalin.model.common.Hand.HandType;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager.WebsoketSessionsTable.WebSocketSession;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.h2.H2BasicTable;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.concurrent.ForkJoinPoolUtils;
import org.nkjmlab.util.java.json.JsonMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.javalin.websocket.WsMessageContext;

public class WebsocketSessionsManager {

  private static final JacksonMapper mapper = GoApplication.getDefaultJacksonMapper();

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final WebsoketSessionsTable websoketSessionsTable;

  private final Queue<String> globalMessages = new ConcurrentLinkedQueue<>();
  private final WebSocketJsonSenderService jsonSenderService = new WebSocketJsonSenderService();

  private final GoTables goTables;

  public WebsocketSessionsManager(GoTables goTables, DataSource memDbDataSource) {
    this.goTables = goTables;
    this.websoketSessionsTable = new WebsoketSessionsTable(memDbDataSource);
    this.websoketSessionsTable.createTableIfNotExists().createIndexesIfNotExists();
  }

  public void onMessage(String gameId, WsMessageContext ctx) {
    GameState gs = ctx.messageAsClass(GameState.class);
    sendGameState(gameId, gs);
  }

  public void onClose(Session session, int statusCode, String reason) {
    session.close();
    log.info("@{} is closed. status code={}, reason={}", session.hashCode(), statusCode, reason);

    websoketSessionsTable
        .removeSession(session)
        .ifPresent(
            gameId -> {
              sendLatestGameStateToSessions(gameId);
              sendEntriesToSessions(gameId);
            });
  }

  public void updateSession(int sessionId, String gameId, String userId) {
    websoketSessionsTable.updateSession(sessionId, gameId, userId);
  }

  public void onConnect(Session session, String userId, String gameId) {

    if (!goTables.usersTable.exists(userId)) {
      log.info("userId=[{}] dose not exists", userId);
      jsonSenderService.submitRequestToLogin(session, userId);
      return;
    }
    websoketSessionsTable.registerSession(gameId, userId, session);

    jsonSenderService.submitInitSession(
        session, session.hashCode(), goTables.usersTable.selectByPrimaryKey(userId));

    Optional.ofNullable(goTables.handsUpTable.selectByPrimaryKey(gameId))
        .ifPresent(
            h ->
                jsonSenderService.submitHandUpOrDown(
                    List.of(session), true, goTables.handsUpTable.readOrder(gameId)));

    Optional.ofNullable(goTables.matchingRequestsTable.selectByPrimaryKey(userId))
        .ifPresent(h -> jsonSenderService.submitUpdateWaitingRequestStatus(List.of(session)));

    jsonSenderService.submitGlobalMessages(session, globalMessages);
    sendEntriesToSessions(gameId);
    sendLatestGameStateToSessions(gameId);
  }

  public void goBack(String gameId) {
    goTables.gameStatesTables.deleteLatestGameState(gameId);
    sendLatestGameStateToSessions(gameId);
  }

  public ProblemJson loadProblem(String gameId, long problemId) {
    Problem p = goTables.problemsTable.selectByPrimaryKey(problemId);
    Hand[] handHistory = mapper.toObject(p.handHistory(), Hand[].class);
    Hand lastHand =
        handHistory.length != 0 ? handHistory[handHistory.length - 1] : Hand.createDummyHand();

    GameState json =
        new GameState(
            -1,
            LocalDateTime.now(),
            gameId,
            "",
            "",
            lastHand,
            mapper.toObject(p.agehama(), Agehama.class),
            mapper.toObject(p.cells(), Integer[][].class),
            mapper.toObject(p.symbols(), new TypeReference<Map<String, Integer>>() {}),
            handHistory,
            p.id(),
            new HashMap<>());
    sendGameState(gameId, json);
    return ProblemJson.createFrom(goTables.problemsTable.selectByPrimaryKey(problemId));
  }

  public void newGame(String gameId, GameState json) {
    GameState newGameJson =
        goTables.gameStatesTables.createNewGameState(
            gameId, json.blackPlayerId(), json.whitePlayerId(), json.cells().length);
    sendGameState(gameId, newGameJson);
  }

  private void sendEntriesToSessions(String gameId) {
    List<UserJson> users =
        websoketSessionsTable.readUsers(goTables.usersTable, gameId).stream()
            .map(u -> new UserJson(u, true))
            .collect(Collectors.toList());

    jsonSenderService.submitEntries(websoketSessionsTable.getSessionsByGameId(gameId), users);
  }

  public void sendGameState(String gameId, GameState json) {
    GameState newJson = removeHagashi(json);

    goTables.gameStatesTables.saveGameState(newJson);
    sendGameStateToSessions(gameId, newJson);
  }

  private GameState removeHagashi(GameState json) {
    List<Hand> history = Arrays.asList(json.handHistory());
    if (history.size() <= 2) {
      return json;
    }
    Hand last = history.get(history.size() - 1);
    Hand second = history.get(history.size() - 2);
    Hand third = history.get(history.size() - 3);
    if (last.stone() == second.stone()
        && second.stone() == third.stone()
        && third.type().equals(HandType.PUT_ON_BOARD.getTypeName())
        && second.type().equals(HandType.REMOVE_FROM_BOARD.getTypeName())
        && third.x() == second.x()
        && third.y() == second.y()) {
      List<Hand> modify = new ArrayList<>(history.subList(0, history.size() - 3));
      modify.add(last);
      return json.updateHandHistory(modify);
    }
    return json;
  }

  private void sendGameStateToSessions(String gameId, GameState json) {
    jsonSenderService.submitGameState(websoketSessionsTable.getSessionsByGameId(gameId), json);
  }

  public void sendGlobalMessage(String message) {
    globalMessages.add(message);
    jsonSenderService.submitGlobalMessage(websoketSessionsTable.getAllSessions(), message);
  }

  public void sendHandDown(String gameId) {
    sendHandUpToSessions(gameId, false, -1);
  }

  public void sendHandUp(String gameId, boolean handUp, int order) {
    sendHandUpToSessions(gameId, handUp, order);
  }

  public void sendHandUpOrder(String gameId, int order) {
    jsonSenderService.submitHandUpOrder(websoketSessionsTable.getSessionsByGameId(gameId), order);
  }

  private void sendHandUpToSessions(String gameId, boolean handUp, int order) {
    jsonSenderService.submitHandUpOrDown(
        websoketSessionsTable.getSessionsByGameId(gameId), handUp, order);
    jsonSenderService.submitUpdateHandUpTable(
        websoketSessionsTable.getAdminSessions(goTables.usersTable));
  }

  public void sendLatestGameStateToSessions(String gameId) {
    sendGameStateToSessions(gameId, goTables.gameStatesTables.readLatestGameState(gameId));
  }

  public void sendUpdateWaitingRequestStatus(Set<String> userIds) {
    if (userIds.size() == 0) {
      return;
    }
    jsonSenderService.submitUpdateWaitingRequestStatus(
        websoketSessionsTable.getSessionsByUserIds(userIds.stream().toList()));
  }

  public void onError(Session session, Throwable cause) {
    log.error(cause);
  }

  private static class WebSocketJsonSenderService {
    private static final org.apache.logging.log4j.Logger log =
        org.apache.logging.log4j.LogManager.getLogger();

    private static final ExecutorService srv =
        Executors.newFixedThreadPool(ForkJoinPoolUtils.getAvailableProcessorsMinus(2));
    private static final JsonMapper mapper = GoApplication.getDefaultJacksonMapper();

    private enum MethodName {
      GAME_STATE,
      INIT_SESSION,
      GLOBAL_MESSAGE,
      HAND_UP,
      HAND_UP_ORDER,
      ENTRIES,
      UPDATE_HAND_UP_TABLE,
      UPDATE_WAITING_REQUEST_STATUS,
      REQUEST_TO_LOGIN
    }

    public WebSocketJsonSenderService() {}

    public void submitGameState(List<Session> sessions, GameState json) {
      submit(sessions, MethodName.GAME_STATE, json);
    }

    public void submitInitSession(Session session, int hashCode, User user) {
      submit(session, MethodName.INIT_SESSION, Map.of("sessionId", hashCode, "user", user));
    }

    public void submitGlobalMessage(List<Session> sessions, String message) {
      submit(sessions, MethodName.GLOBAL_MESSAGE, List.of(message));
    }

    public void submitGlobalMessages(Session session, Collection<String> messages) {
      submit(session, MethodName.GLOBAL_MESSAGE, messages);
    }

    public void submitHandUpOrDown(List<Session> sessions, boolean handUp, int order) {
      submit(sessions, MethodName.HAND_UP, Map.of("handUp", handUp, "order", order));
    }

    public void submitEntries(List<Session> sessions, List<UserJson> users) {
      submit(sessions, MethodName.ENTRIES, users);
    }

    public void submitHandUpOrder(List<Session> sessions, int order) {
      submit(sessions, MethodName.HAND_UP_ORDER, order);
    }

    public void submitUpdateHandUpTable(List<Session> sessions) {
      submit(sessions, MethodName.UPDATE_HAND_UP_TABLE, true);
    }

    public void submitUpdateWaitingRequestStatus(List<Session> sessions) {
      submit(sessions, MethodName.UPDATE_WAITING_REQUEST_STATUS, true);
    }

    public void submitRequestToLogin(Session session, String userId) {
      submit(session, MethodName.REQUEST_TO_LOGIN, userId);
    }

    private void submit(List<Session> sessions, MethodName methodName, Object json) {
      String text = mapper.toJson(new WebsocketJson(methodName.toString(), json));
      sessions.forEach(session -> submitText(session, methodName, text));
    }

    private void submit(Session session, MethodName methodName, Object json) {
      String text = mapper.toJson(new WebsocketJson(methodName.toString(), json));
      submitText(session, methodName, text);
    }

    private void submitText(Session session, MethodName methodName, String text) {
      srv.submit(() -> sendText(session, text));
    }

    private void sendText(Session session, String text) {
      RemoteEndpoint b = session.getRemote();
      synchronized (b) {
        b.sendString(
            text,
            new WriteCallback() {
              @Override
              public void writeFailed(Throwable x) {
                try {
                  b.sendString(text);
                } catch (IOException e) {
                  log.error(e.getMessage());
                }
              }

              @Override
              public void writeSuccess() {}
            });
      }
    }

    private static class WebsocketJson {
      public final String method;
      public final Object content;

      public WebsocketJson(String method, Object content) {
        this.method = method;
        this.content = content;
      }

      @Override
      public String toString() {
        return "WebsocketJson [method=" + method + ", content=" + content + "]";
      }
    }
  }

  public static class WebsoketSessionsTable extends H2BasicTable<WebSocketSession> {

    private static final String USER_ID = "user_id";
    private static final String GAME_ID = "game_id";

    private static Map<Integer, Session> sessions = new ConcurrentHashMap<>();

    public WebsoketSessionsTable(DataSource dataSource) {
      super(Sorm.create(dataSource), WebSocketSession.class);
    }

    List<WebSocketSession> readSessionsByGameId(String gameId) {
      return readList(SELECT_STAR + FROM + getTableName() + WHERE + GAME_ID + "=?", gameId);
    }

    List<WebSocketSession> readSessionsByUserId(String userId) {
      return readList("select * " + FROM + getTableName() + WHERE + USER_ID + "=?", userId);
    }

    List<Session> getSessionsByGameId(String gameId) {
      List<Session> result =
          readSessionsByGameId(gameId).stream()
              .map(session -> sessions.get(session.sessionId()))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      return result;
    }

    List<Session> getSessionsByUserId(String userId) {
      List<Session> result =
          readSessionsByUserId(userId).stream()
              .map(session -> sessions.get(session.sessionId()))
              .filter(s -> Objects.nonNull(s))
              .collect(Collectors.toList());
      return result;
    }

    public List<Session> getSessionsByUserIds(List<String> userIds) {
      if (userIds.size() == 0) {
        return Collections.emptyList();
      }
      ParameterizedSql psql =
          ParameterizedSqlParser.parse(
              "select * from " + getTableName() + " where " + USER_ID + " IN(<?>) ", userIds);
      return readList(psql.getSql(), psql.getParameters()).stream()
          .map(session -> sessions.get(session.sessionId()))
          .filter(s -> Objects.nonNull(s))
          .collect(Collectors.toList());
    }

    List<Session> getAllSessions() {
      List<Session> result =
          selectAll().stream()
              .map(session -> sessions.get(session.sessionId()))
              .filter(s -> Objects.nonNull(s))
              .collect(Collectors.toList());
      return result;
    }

    void registerSession(String gameId, String userId, Session session) {
      int sessionId = session.hashCode();
      WebSocketSession ws = new WebSocketSession(sessionId, userId, gameId, LocalDateTime.now());
      if (exists(ws)) {
        log.warn("{} already exists.", ws);
        return;
      }
      insert(ws);
      sessions.put(sessionId, session);
      log.info("WebSocket is registered={}", ws);
    }

    void updateSession(int sessionId, String gameId, String userId) {
      update(new WebSocketSession(sessionId, userId, gameId, LocalDateTime.now()));
    }

    Optional<String> removeSession(Session session) {
      if (sessions.entrySet().removeIf(e -> e.getKey() == session.hashCode())) {
        WebSocketSession gs = selectByPrimaryKey(session.hashCode());
        delete(gs);
        return Optional.of(gs.gameId());
      }
      return Optional.empty();
    }

    public List<User> readUsers(UsersTable usersTable, String gameId) {
      Set<String> uids =
          readSessionsByGameId(gameId).stream()
              .map(u -> u == null ? null : u.userId())
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      return usersTable.readListByUids(uids);
    }

    public List<User> readStudents(UsersTable usersTable, String gameId) {
      return readUsers(usersTable, gameId).stream()
          .filter(u -> u.isStudent())
          .collect(Collectors.toList());
    }

    public List<String> readActiveGameIdsOrderByGameId(UsersTable usersTable) {
      ParameterizedSql psql =
          ParameterizedSqlParser.parse(
              SELECT + DISTINCT + GAME_ID + FROM + getTableName() + WHERE + GAME_ID + LIKE + "?",
              "%-vs-%");
      return getOrm().readList(String.class, psql).stream()
          .filter(gid -> readUsers(usersTable, gid).size() > 0)
          .sorted()
          .collect(Collectors.toList());
    }

    public int getWatchingUniqueStudentsNum(UsersTable usersTable, String gameId) {
      return (int)
          readStudents(usersTable, gameId).stream()
              .map(uj -> uj.userId())
              .collect(Collectors.toSet())
              .stream()
              .count();
    }

    public List<Session> getAdminSessions(UsersTable usersTable) {
      List<String> ids = usersTable.getAdminUserIds();
      if (ids.size() == 0) {
        return Collections.emptyList();
      }
      ParameterizedSql st =
          OrderedParameterSqlParser.parse(
              "select * from " + getTableName() + " where " + USER_ID + " IN (<?>)", ids);
      return readList(st.getSql(), st.getParameters()).stream()
          .map(ws -> sessions.get(ws.sessionId()))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    }

    @OrmRecord
    public static record WebSocketSession(
        @PrimaryKey int sessionId, String userId, @Index String gameId, LocalDateTime createdAt) {}
  }

  public int getWatchingUniqueStudentsNum(String gameId) {
    return websoketSessionsTable.getWatchingUniqueStudentsNum(goTables.usersTable, gameId);
  }

  public List<String> readActiveGameIdsOrderByGameId() {
    return websoketSessionsTable.readActiveGameIdsOrderByGameId(goTables.usersTable);
  }
}
