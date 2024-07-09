package org.nkjmlab.go.javalin.handler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.nkjmlab.go.javalin.GoAccessManager.AccessRole;
import org.nkjmlab.go.javalin.handler.FirebaseConfigs.FirebaseConfig;
import org.nkjmlab.go.javalin.handler.GoGetHandler.GoViewHandler;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService.SigninSession;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.util.java.web.ViewModel.Builder;
import org.nkjmlab.util.java.web.WebApplicationFileLocation;

import io.javalin.Javalin;
import io.javalin.http.Handler;

public class GoGetHandlers {

  private final Javalin app;
  private final WebsocketSessionsManager websocketManager;
  private final WebApplicationFileLocation webAppFileLocation;
  private final FirebaseConfig firebaseConfig;
  private final GoTables goTables;
  private final GoAuthService authService;
  private final boolean usePopupSignin;

  public GoGetHandlers(
      Javalin app,
      WebsocketSessionsManager websocketManager,
      WebApplicationFileLocation webAppConfig,
      FirebaseConfig firebaseConfig,
      GoTables goTables,
      GoAuthService authService,
      boolean usePopupSignin) {
    this.app = app;
    this.websocketManager = websocketManager;
    this.webAppFileLocation = webAppConfig;
    this.firebaseConfig = firebaseConfig;
    this.goTables = goTables;
    this.authService = authService;
    this.usePopupSignin = usePopupSignin;
  }

  public void prepareGetHandlers() {

    app.get("/app/play.html", createPlayHandler(), AccessRole.LOGIN_ROLES);
    app.get("/app/players-all.html", createPlayersAllHandler(), AccessRole.ADMIN);

    app.get("/app/admin.html", createGoHandler(new AdminViewHandler()), AccessRole.ADMIN);
    app.get("/app/players.html", createPlayersHandler(), AccessRole.ADMIN);
    app.get("/app/games-all.html", createGamesAllHandler(), AccessRole.ADMIN);
    app.get("/app/games.html", createGamesHandler(), AccessRole.ADMIN);
    app.get(
        "/app/fragment/game-record-table.html",
        createGameRecordTableHandler(),
        AccessRole.LOGIN_ROLES);
    app.get("/app/fragment/question-table*", createQuestionTableHandler(), AccessRole.ADMIN);

    app.get(
        "/app/fragment/waiting-request-table.html",
        createWaitingRequestHandler(),
        AccessRole.ADMIN);

    app.get(
        "/app/fragment/waiting-request-table-small.html",
        createWatingRequestSmallHandler(),
        AccessRole.LOGIN_ROLES);

    app.get("/app", ctx -> ctx.redirect("/app/index.html"));
    app.get(
        "/app/index.html",
        createGoHandler(
            ctx ->
                filePath ->
                    model -> {
                      model.put(
                          "popupSignin",
                          ctx.userAgent().toLowerCase().contains("ios") || usePopupSignin);
                      ctx.render(filePath, model.build());
                    }));
    app.get(
        "/app/*",
        createGoHandler(
            ctx ->
                filePath ->
                    model -> {
                      ctx.render(filePath, model.build());
                    }));
  }

  Handler createGoHandler(GoViewHandler handler) {
    return new GoGetHandler(
        webAppFileLocation.webRootDirectory(), goTables, authService, firebaseConfig, handler);
  }

  private Handler createPlayHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  Optional<SigninSession> session =
                      authService.toSigninSession(ctx.req().getSession().getId());
                  session.ifPresent(
                      opts -> {
                        boolean attend = goTables.loginsTable.isAttendance(opts.userId());
                        model.put("isAttendance", attend);
                        model.put(
                            "problemGroupsJson", goTables.problemsTable.getProblemGroupsNode());
                        ctx.render(filePath, model.build());
                      });
                });
  }

  private Handler createPlayersAllHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  putUserAccounts(model);
                  ctx.render("players.html", model.build());
                });
  }

  private Handler createPlayersHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  putUserAccounts(model);
                  ctx.render(filePath, model.build());
                });
  }

  private void putUserAccounts(Builder model) {
    List<Tuple2<User, Login>> users = goTables.usersTable.readAllWithLastLogin();
    List<GoGetHandlers.LoginJson> loginJsons =
        users.stream()
            .filter(t -> t.getT1().isStudent())
            .map(t -> new LoginJson(t.getT2(), t.getT1()))
            .collect(Collectors.toList());
    model.put("userAccounts", loginJsons);
  }

  private Handler createGamesAllHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  List<GameStateViewJson> tmp =
                      goTables.gameStatesTables.readTodayGameJsons().stream()
                          .map(
                              gsj -> {
                                String gameId = gsj.gameId();
                                GameStateViewJson json =
                                    new GameStateViewJson(
                                        gsj,
                                        goTables.handsUpTable.selectByPrimaryKey(gameId),
                                        websocketManager.getWatchingUniqueStudentsNum(gameId));
                                return json;
                              })
                          .collect(Collectors.toList());
                  model.put("games", tmp);
                  ctx.render("games.html", model.build());
                });
  }

  private Handler createGamesHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  List<String> gids = websocketManager.readActiveGameIdsOrderByGameId();
                  List<GameStateViewJson> tmp =
                      gids.stream()
                          .map(gid -> goTables.gameStatesTables.readLatestGameState(gid))
                          .map(
                              gsj ->
                                  new GameStateViewJson(
                                      gsj,
                                      goTables.handsUpTable.selectByPrimaryKey(gsj.gameId()),
                                      websocketManager.getWatchingUniqueStudentsNum(gsj.gameId())))
                          .collect(Collectors.toList());
                  model.put(
                      "games",
                      tmp.stream()
                          .filter(j -> j.watchingStudentsNum() > 0)
                          .collect(Collectors.toList()));
                  ctx.render(filePath, model.build());
                });
  }

  private Handler createGameRecordTableHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  String userId = ctx.queryParam("userId");
                  List<GameRecord> records = goTables.gameRecordsTable.readByUserId(userId);
                  model.put("records", records);
                  ctx.render("fragment/game-record-table.html", model.build());
                });
  }

  private Handler createQuestionTableHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  List<String> gids = goTables.handsUpTable.readAllGameIds();
                  List<GameStateViewJson> tmp =
                      goTables.gameStatesTables.readLatestBoardsJson(gids).stream()
                          .map(
                              gsj ->
                                  new GameStateViewJson(
                                      gsj,
                                      goTables.handsUpTable.selectByPrimaryKey(gsj.gameId()),
                                      0))
                          .toList();
                  model.put("games", tmp);
                  ctx.render(filePath, model.build());
                });
  }

  private Handler createWaitingRequestHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  String userId = ctx.queryParam("userId");
                  if (userId != null) {
                    List<MatchingRequest> tmp = goTables.matchingRequestsTable.readRequests();
                    model.put(
                        "requests",
                        tmp.stream()
                            .filter(r -> r.userId().equals(userId))
                            .collect(Collectors.toList()));
                  } else {
                    model.put("requests", goTables.matchingRequestsTable.readRequests());
                  }
                  ctx.render(filePath, model.build());
                });
  }

  private Handler createWatingRequestSmallHandler() {
    return createGoHandler(
        ctx ->
            filePath ->
                model -> {
                  String userId = ctx.queryParam("userId");
                  List<MatchingRequest> tmp = goTables.matchingRequestsTable.readRequests();
                  MatchingRequest req =
                      tmp.stream()
                          .filter(r -> r.userId().equals(userId))
                          .findAny()
                          .orElse(new MatchingRequest());
                  model.put("req", req);
                  model.put("reqNum", tmp.size());
                  ctx.render(filePath, model.build());
                });
  }

  public static record LoginJson(Login login, User user) {}

  public static record GameStateViewJson(
      GameState gameState, HandUp handUp, int watchingStudentsNum) {}
}
