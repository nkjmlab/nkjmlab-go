package org.nkjmlab.go.javalin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.nkjmlab.go.javalin.GoAccessManager.UserRole;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.util.jakarta.servlet.UserSession;
import org.nkjmlab.util.java.net.UrlUtils;
import org.nkjmlab.util.java.web.ViewModel;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import jakarta.servlet.http.HttpServletRequest;

public class GoAppHandlers {

  public static class GoHandler implements Handler {

    private final Function<GoTables, Function<Context, Function<String, Function<ViewModel.Builder, Consumer<UserSession>>>>> handler;
    private final GoTables goTables;

    public GoHandler(GoTables goTables,
        Function<GoTables, Function<Context, Function<String, Function<ViewModel.Builder, Consumer<UserSession>>>>> handler) {
      this.handler = handler;
      this.goTables = goTables;
    }

    @Override
    public void handle(Context ctx) throws Exception {
      String filePath = UrlUtils.of(ctx.url()).getPath().replaceFirst("^/app/", "");
      ViewModel.Builder model = createDefaultViewModelBuilder(goTables.usersTable, ctx.req());
      UserSession session = UserSession.wrap(ctx.req().getSession());
      handler.apply(goTables).apply(ctx).apply(filePath).apply(model).accept(session);
    }

    private static ViewModel.Builder createDefaultViewModelBuilder(UsersTable usersTable,
        HttpServletRequest request) {
      Map<String, Object> map = ViewModel.builder()
          .setFileModifiedDate(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory(), 10, "js", "css")
          .put("webjars", GoWebAppConfig.WEB_APP_CONFIG.getWebJars())
          .put("currentUser", getCurrentUserAccount(usersTable, request)).build();
      return ViewModel.builder(map);
    }

    private static User getCurrentUserAccount(UsersTable usersTable, HttpServletRequest request) {
      Optional<User> u = UserSession.wrap(request.getSession()).getUserId()
          .map(uid -> usersTable.selectByPrimaryKey(uid));
      return u.orElse(new User());
    }
  }

  private static class PlayGoHandler extends GoHandler {

    public PlayGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        session.getUserId().ifPresent(uid -> {
          boolean attend = gtbl.loginsTable.isAttendance(uid);
          model.put("isAttendance", attend);
          model.put("problemGroupsJson", gtbl.problemsTable.getProblemGroupsNode());
        });
        ctx.render(filePath, model.build());
      });

    }
  }

  private static class AppIndexGoHandler extends GoHandler {
    public AppIndexGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        session.getUserId().ifPresent(uid -> {
          boolean attend = gtbl.loginsTable.isAttendance(uid);
          model.put("isAttendance", attend);
          model.put("problemGroupsJson", gtbl.problemsTable.getProblemGroupsNode());
        });
        ctx.render(filePath, model.build());
      });
    }
  }

  private static class PlayersAllGoHandler extends GoHandler {
    public PlayersAllGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        List<Tuple2<User, Login>> users = gtbl.usersTable.readAllWithLastLogin();
        List<GoAppHandlers.LoginJson> loginJsons =
            users.stream().map(t -> new GoAppHandlers.LoginJson(t.getT2(), t.getT1()))
                .collect(Collectors.toList());
        model.put("userAccounts", loginJsons);
        ctx.render("players.html", model.build());
      });
    }
  }
  private static class PlayersGoHandler extends GoHandler {
    public PlayersGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        List<Tuple2<User, Login>> users = gtbl.usersTable.readAllWithLastLogin();
        List<GoAppHandlers.LoginJson> loginJsons = users.stream().filter(t -> t.getT1().isStudent())
            .map(t -> new GoAppHandlers.LoginJson(t.getT2(), t.getT1()))
            .collect(Collectors.toList());
        model.put("userAccounts", loginJsons);
        ctx.render(filePath, model.build());
      });
    }
  }
  private static class GamesAllGoHandler extends GoHandler {
    public GamesAllGoHandler(GoTables goTables, WebsocketSessionsManager websocketManager) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        List<GoAppHandlers.GameStateViewJson> tmp =
            gtbl.gameStatesTables.readTodayGameJsons().stream().map(gsj -> {
              String gameId = gsj.gameId();
              GoAppHandlers.GameStateViewJson json = new GoAppHandlers.GameStateViewJson(gsj,
                  gtbl.handsUpTable.selectByPrimaryKey(gameId),
                  websocketManager.getWatchingUniqueStudentsNum(gameId));
              return json;
            }).collect(Collectors.toList());
        model.put("games", tmp);
        ctx.render("games.html", model.build());
      });
    }
  }
  private static class GamesGoHandler extends GoHandler {
    public GamesGoHandler(GoTables goTables, WebsocketSessionsManager websocketManager) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        List<String> gids = websocketManager.readActiveGameIdsOrderByGameId();
        List<GoAppHandlers.GameStateViewJson> tmp =
            gids.stream().map(gid -> gtbl.gameStatesTables.readLatestGameState(gid))
                .map(gsj -> new GoAppHandlers.GameStateViewJson(gsj,
                    gtbl.handsUpTable.selectByPrimaryKey(gsj.gameId()),
                    websocketManager.getWatchingUniqueStudentsNum(gsj.gameId())))
                .collect(Collectors.toList());
        model.put("games",
            tmp.stream().filter(j -> j.watchingStudentsNum() > 0).collect(Collectors.toList()));
        ctx.render(filePath, model.build());
      });
    }
  }
  private static class GameRecordTableGoHandler extends GoHandler {
    public GameRecordTableGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        String userId = ctx.queryParam("userId");
        List<GameRecord> records = gtbl.gameRecordsTable.readByUserId(userId);
        model.put("records", records);
        ctx.render("fragment/game-record-table.html", model.build());
      });
    }
  }

  private static class QuestionTableGoHandler extends GoHandler {
    public QuestionTableGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        List<String> gids = gtbl.handsUpTable.readAllGameIds();
        List<GoAppHandlers.GameStateViewJson> tmp = gtbl.gameStatesTables.readLatestBoardsJson(gids)
            .stream().map(gsj -> new GoAppHandlers.GameStateViewJson(gsj,
                gtbl.handsUpTable.selectByPrimaryKey(gsj.gameId()), 0))
            .toList();
        model.put("games", tmp);
        ctx.render(filePath, model.build());
      });
    }
  }

  private static class WaitingRequestGoHandler extends GoHandler {
    public WaitingRequestGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        String userId = ctx.queryParam("userId");
        if (userId != null) {
          List<MatchingRequest> tmp = gtbl.matchingRequestsTable.readRequests();
          model.put("requests",
              tmp.stream().filter(r -> r.userId().equals(userId)).collect(Collectors.toList()));
        } else {
          model.put("requests", gtbl.matchingRequestsTable.readRequests());
        }
        ctx.render(filePath, model.build());
      });
    }
  }
  private static class WaitingRequestSmallGoHandler extends GoHandler {
    public WaitingRequestSmallGoHandler(GoTables goTables) {
      super(goTables, gtbl -> ctx -> filePath -> model -> session -> {
        String userId = ctx.queryParam("userId");
        List<MatchingRequest> tmp = gtbl.matchingRequestsTable.readRequests();
        MatchingRequest req = tmp.stream().filter(r -> r.userId().equals(userId)).findAny()
            .orElse(new MatchingRequest());
        model.put("req", req);
        model.put("reqNum", tmp.size());
        ctx.render(filePath, model.build());
      });
    }
  }


  public static void prepareGetHandler(Javalin app, WebsocketSessionsManager websocketManager,
      GoTables goTables) {

    app.get("/app", ctx -> ctx.redirect("/app/index.html"));
    app.get("/app/index.html", new AppIndexGoHandler(goTables));
    app.get("/app/play.html", new PlayGoHandler(goTables), UserRole.LOGIN_ROLES);
    app.get("/app/players-all.html", new PlayersAllGoHandler(goTables), UserRole.ADMIN);
    app.get("/app/players.html", new PlayersGoHandler(goTables), UserRole.ADMIN);
    app.get("/app/games-all.html", new GamesAllGoHandler(goTables, websocketManager),
        UserRole.ADMIN);
    app.get("/app/games.html", new GamesGoHandler(goTables, websocketManager), UserRole.ADMIN);
    app.get("/app/fragment/game-record-table.html", new GameRecordTableGoHandler(goTables),
        UserRole.ADMIN);
    app.get("/app/fragment/question-table*", new QuestionTableGoHandler(goTables), UserRole.ADMIN);

    app.get("/app/fragment/waiting-request-table.html", new WaitingRequestGoHandler(goTables),
        UserRole.ADMIN);

    app.get("/app/fragment/waiting-request-table-small.html",
        new WaitingRequestSmallGoHandler(goTables), UserRole.ADMIN);

    app.get("/app/*", new GoHandler(goTables, gtbl -> ctx -> filePath -> model -> session -> {
      ctx.render(filePath, model.build());
    }));

  }

  public static record LoginJson(Login login, User user) {

  }

  public static record GameStateViewJson(GameState gameState, HandUp handUp,
      int watchingStudentsNum) {

  }


}
