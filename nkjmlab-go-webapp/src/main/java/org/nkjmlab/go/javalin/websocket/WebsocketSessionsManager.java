package org.nkjmlab.go.javalin.websocket;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.nkjmlab.go.javalin.GoApplication;
import org.nkjmlab.go.javalin.model.common.Agehama;
import org.nkjmlab.go.javalin.model.common.Hand;
import org.nkjmlab.go.javalin.model.common.Hand.HandType;
import org.nkjmlab.go.javalin.model.common.ProblemJson;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameStateJson;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable.Problem;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.UsersTable.UserJson;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.concurrent.ForkJoinPoolUtils;
import org.nkjmlab.util.java.json.JsonMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.javalin.websocket.WsMessageContext;

public class WebsocketSessionsManager {

  private static final JacksonMapper mapper = GoApplication.getDefaultJacksonMapper();

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final ProblemsTable problemsTable;
  private final GameStatesTables gameStatesTables;
  private final WebsoketSessionsTable websoketSessionsTable;
  private final UsersTable usersTable;
  private final HandUpsTable handsUpTable;
  private final MatchingRequestsTable matchingRequestsTable;


  private final Queue<String> globalMessages = new ConcurrentLinkedQueue<>();
  private final WebSocketJsonSenderService jsonSenderService = new WebSocketJsonSenderService();


  public WebsocketSessionsManager(GameStatesTables gameStatesTables, ProblemsTable problemsTable,
      WebsoketSessionsTable websoketSessionsTable, UsersTable usersTable, HandUpsTable handsUpTable,
      MatchingRequestsTable matchingRequestsTable) {
    this.gameStatesTables = gameStatesTables;
    this.problemsTable = problemsTable;
    this.websoketSessionsTable = websoketSessionsTable;
    this.usersTable = usersTable;
    this.handsUpTable = handsUpTable;
    this.matchingRequestsTable = matchingRequestsTable;
  }


  public void onMessage(String gameId, WsMessageContext ctx) {
    GameStateJson gs = ctx.messageAsClass(GameStateJson.class);
    sendGameState(gameId, gs);
  }

  public void onClose(Session session, int statusCode, String reason) {
    session.close();
    log.info("@{} is closed. status code={}, reason={}", session.hashCode(), statusCode, reason);

    websoketSessionsTable.removeSession(session).ifPresent(gameId -> {
      sendLatestGameStateToSessions(gameId);
      sendEntriesToSessions(gameId);
    });

  }

  public void updateSession(int sessionId, String gameId, String userId) {
    websoketSessionsTable.updateSession(sessionId, gameId, userId);
  }


  public void onConnect(Session session, String userId, String gameId) {

    if (!usersTable.exists(userId)) {
      log.info("userId=[{}] dose not exists", userId);
      jsonSenderService.submitRequestToLogin(session, userId);
      return;
    }
    websoketSessionsTable.registerSession(gameId, userId, session);

    jsonSenderService.submitInitSession(session, session.hashCode(),
        usersTable.selectByPrimaryKey(userId));

    Optional.ofNullable(handsUpTable.selectByPrimaryKey(gameId)).ifPresent(h -> jsonSenderService
        .submitHandUpOrDown(List.of(session), true, handsUpTable.readOrder(gameId)));

    Optional.ofNullable(matchingRequestsTable.selectByPrimaryKey(userId))
        .ifPresent(h -> jsonSenderService.submitUpdateWaitingRequestStatus(List.of(session)));

    jsonSenderService.submitGlobalMessages(session, globalMessages);
    sendEntriesToSessions(gameId);
    sendLatestGameStateToSessions(gameId);
  }


  public void goBack(String gameId) {
    gameStatesTables.deleteLatestGameState(gameId);
    sendLatestGameStateToSessions(gameId);
  }


  public ProblemJson loadProblem(String gameId, long problemId) {
    Problem p = problemsTable.selectByPrimaryKey(problemId);
    Hand[] handHistory = mapper.toObject(p.handHistory(), Hand[].class);
    Hand lastHand =
        handHistory.length != 0 ? handHistory[handHistory.length - 1] : Hand.createDummyHand();
    GameStateJson json =
        new GameStateJson(-1, gameId, "", "", mapper.toObject(p.cells(), int[][].class),
            mapper.toObject(p.symbols(), new TypeReference<Map<String, Integer>>() {}),
            mapper.toObject(p.agehama(), Agehama.class), lastHand, handHistory, p.id(),
            new HashMap<>(), LocalDateTime.now());
    sendGameState(gameId, json);
    return ProblemJson.createFrom(problemsTable.selectByPrimaryKey(problemId));
  }



  public void newGame(String gameId, GameStateJson json) {
    GameStateJson newGameJson = gameStatesTables.createNewGameState(gameId, json.blackPlayerId(),
        json.whitePlayerId(), json.cells().length);
    sendGameState(gameId, newGameJson);
  }

  private void sendEntriesToSessions(String gameId) {
    List<UserJson> users = websoketSessionsTable.readUsers(usersTable, gameId).stream()
        .map(u -> new UserJson(u, true)).collect(Collectors.toList());

    jsonSenderService.submitEntries(websoketSessionsTable.getSessionsByGameId(gameId), users);

  }

  public void sendGameState(String gameId, GameStateJson json) {
    removeHagashi(json);

    gameStatesTables.saveGameState(json);
    sendGameStateToSessions(gameId, json);
  }

  private GameStateJson removeHagashi(GameStateJson json) {
    List<Hand> history = Arrays.asList(json.handHistory());
    if (history.size() <= 2) {
      return json;
    }
    Hand last = history.get(history.size() - 1);
    Hand second = history.get(history.size() - 2);
    Hand third = history.get(history.size() - 3);
    if (last.stone() == second.stone() && second.stone() == third.stone()
        && third.type().equals(HandType.PUT_ON_BOARD.getTypeName())
        && second.type().equals(HandType.REMOVE_FROM_BOARD.getTypeName()) && third.x() == second.x()
        && third.y() == second.y()) {
      List<Hand> modify = history.subList(0, history.size() - 3);
      modify.add(last);
      return json.updateHandHistory(modify);
    }
    return json;
  }


  private void sendGameStateToSessions(String gameId, GameStateJson json) {
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
    jsonSenderService.submitHandUpOrDown(websoketSessionsTable.getSessionsByGameId(gameId), handUp,
        order);
    jsonSenderService.submitUpdateHandUpTable(websoketSessionsTable.getAdminSessions(usersTable));
  }

  public void sendLatestGameStateToSessions(String gameId) {
    sendGameStateToSessions(gameId, gameStatesTables.readLatestGameStateJson(gameId));
  }



  public void sendUpdateWaitingRequestStatus(List<String> userIds) {
    if (userIds.size() == 0) {
      return;
    }
    jsonSenderService
        .submitUpdateWaitingRequestStatus(websoketSessionsTable.getSessionsByUserIds(userIds));
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
      GAME_STATE, INIT_SESSION, GLOBAL_MESSAGE, HAND_UP, HAND_UP_ORDER, ENTRIES, UPDATE_HAND_UP_TABLE, UPDATE_WAITING_REQUEST_STATUS, REQUEST_TO_LOGIN
    }

    public WebSocketJsonSenderService() {}

    public void submitGameState(List<Session> sessions, GameStateJson json) {
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
        b.sendString(text, new WriteCallback() {
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

}
