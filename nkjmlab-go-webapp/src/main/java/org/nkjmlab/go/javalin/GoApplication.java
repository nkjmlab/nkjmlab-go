package org.nkjmlab.go.javalin;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.GoAccessManager.UserRole;
import org.nkjmlab.go.javalin.auth.AuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable.GameRecord;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable.GameState;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable;
import org.nkjmlab.go.javalin.model.relation.HandUpsTable.HandUp;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable.Login;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable.MatchingRequest;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.sorm4j.util.h2.datasource.H2LocalDataSourceFactory;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProcess;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProperties;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.jakarta.servlet.UserSession;
import org.nkjmlab.util.java.function.Try;
import org.nkjmlab.util.java.io.SystemFileUtils;
import org.nkjmlab.util.java.json.FileDatabaseConfigJson;
import org.nkjmlab.util.java.lang.ProcessUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;
import org.nkjmlab.util.java.lang.SystemPropertyUtils;
import org.nkjmlab.util.java.net.UrlUtils;
import org.nkjmlab.util.java.web.ViewModel;
import org.nkjmlab.util.java.web.WebJarsUtils;
import org.nkjmlab.util.javalin.JavalinJsonRpcService;
import org.nkjmlab.util.thymeleaf.ThymeleafTemplateEnginBuilder;
import org.thymeleaf.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import jakarta.servlet.http.HttpServletRequest;

public class GoApplication {



  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  public static class Const {

    public static final File INITIAL_ICON_DIR = new File(Const.WEBROOT_DIR, "img/icon-initial");
    public static final File BACKUP_DIR = new File(Const.USER_HOME_DIR, "go-bkup/");
    public static final File PROBLEM_DIR = new File(Const.APP_ROOT_DIR, "problem");
    public static final File PROBLEM_BACKUP_DIR = new File(Const.APP_ROOT_DIR, "problem-auto-bkup");
    public static final File CURRENT_ICON_DIR = new File(Const.WEBROOT_DIR, "img/icon");
    public static final File UPLOADED_ICON_DIR = new File(Const.WEBROOT_DIR, "img/icon-uploaded");
    public static final File RANDOM_ICON_DIR = new File(Const.WEBROOT_DIR, "img/icon-random");
    static final File USER_HOME_DIR = SystemFileUtils.getUserHomeDirectory();
    static final File WEBROOT_DIR = new File(Const.APP_ROOT_DIR, Const.WEBROOT_DIR_NAME);
    static final String WEBROOT_DIR_NAME = "/webroot";
    static final File APP_ROOT_DIR = ResourceUtils.getResourceAsFile("/");
    private static final int TRIM_THRESHOLD_OF_GAME_STATE_TABLE = 30000;
    private static final int INTERVAL_IN_WAITING_ROOM = 10;
    private static final int WS_PING_INTERVAL_SEC = 27;
    private static long THYMELEAF_EXPIRE_TIME_MILLI_SECOND = 1 * 1000;

  }

  private final DataSource memDbDataSource;
  private final DataSource fileDbDataSource;

  private final Javalin app;
  private final ProblemsTable problemsTable;
  private final HandUpsTable handsUpTable;
  private final UsersTable usersTable;
  private final PasswordsTable passwordsTable;
  private final MatchingRequestsTable matchingRequestsTable;
  private final GameStatesTables gameStatesTables;
  private final VotesTable votesTable;
  private final GameRecordsTable gameRecordsTable;
  private final LoginsTable loginsTable;
  private final WebsocketSessionsManager webSocketManager;

  private static final Map<String, String> WEB_JARS_VERSIONS = WebJarsUtils
      .findWebJarVersionsFromClassPath("jquery", "sweetalert2", "bootstrap", "bootstrap-treeview",
          "clipboard", "fortawesome__fontawesome-free", "stacktrace-js", "datatables", "firebase",
          "firebaseui", "ua-parser-js", "blueimp-load-image", "emojionearea");

  public static void main(String[] args) {
    if (args.length != 0) {
      Const.THYMELEAF_EXPIRE_TIME_MILLI_SECOND = Long.valueOf(args[0]);
    }
    int port = 4567;
    log.info("start (port:{}) => {}", port, SystemPropertyUtils.getJavaProperties());

    ProcessUtils.stopProcessBindingPortIfExists(port);
    new H2TcpServerProcess(H2TcpServerProperties.builder().build()).awaitStart();

    new GoApplication().start(port);
  }

  private void start(int port) {
    app.start(port);
  }

  public GoApplication() {

    log.info("log4j2.configurationFile={}, Logger level={}",
        System.getProperty("log4j2.configurationFile"), log.getLevel());
    FileDatabaseConfigJson fileDbConf = getFileDbConfig();

    H2LocalDataSourceFactory factory =
        H2LocalDataSourceFactory.builder(fileDbConf.databaseDirectory, fileDbConf.databaseName,
            fileDbConf.username, fileDbConf.password).build();

    factory.makeFileDatabaseIfNotExists();

    this.memDbDataSource = DataSourceUtils.createHikariDataSource(factory.getInMemoryModeJdbcUrl(),
        factory.getUsername(), factory.getPassword());
    log.info("server jdbcUrl={}", factory.getServerModeJdbcUrl());

    this.fileDbDataSource = DataSourceUtils.createHikariDataSource(factory.getServerModeJdbcUrl(),
        factory.getUsername(), factory.getPassword());
    // H2Server.openBrowser(memDbDataSource, true);

    TemplateEngine engine = ThymeleafTemplateEnginBuilder.builder()
        .setTtlMs(Const.THYMELEAF_EXPIRE_TIME_MILLI_SECOND).build();
    JavalinThymeleaf.init(engine);

    {
      this.problemsTable = new ProblemsTable(memDbDataSource);
      problemsTable.dropAndInsertInitialProblemsToTable(Const.PROBLEM_DIR);
    }
    {
      this.loginsTable = new LoginsTable(fileDbDataSource);
      loginsTable.createTableIfNotExists().createIndexesIfNotExists();
      loginsTable
          .writeCsv(new File(Const.BACKUP_DIR, "logins-" + System.currentTimeMillis() + ".csv"));
    }

    this.handsUpTable = new HandUpsTable(memDbDataSource);
    {
      this.usersTable = new UsersTable(fileDbDataSource);
      usersTable.dropTableIfExists();
      usersTable.createTableIfNotExists().createIndexesIfNotExists();
      try {
        File f = ResourceUtils.getResourceAsFile("/conf/users.csv");
        usersTable.readFromFileAndMerge(f);
      } catch (Exception e) {
        log.error(e, e);
        log.warn("load users.csv.default ...");
        File f = ResourceUtils.getResourceAsFile("/conf/users.csv.default");
        usersTable.readFromFileAndMerge(f);
      }
    }
    {
      this.passwordsTable = new PasswordsTable(fileDbDataSource);
      passwordsTable.createTableIfNotExists().createIndexesIfNotExists();
      try {
        File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv");
        passwordsTable.readFromFileAndMerge(f);
      } catch (Exception e) {
        log.warn("load password.csv.default ...");
        File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv.default");
        passwordsTable.readFromFileAndMerge(f);
      }
    }
    {
      this.gameRecordsTable = new GameRecordsTable(fileDbDataSource);
      gameRecordsTable.createTableIfNotExists().createIndexesIfNotExists();
      gameRecordsTable.writeCsv(
          new File(Const.BACKUP_DIR, "game-record" + System.currentTimeMillis() + ".csv"));
      gameRecordsTable.recalculateAndUpdateRank(usersTable);
    }
    {
      this.matchingRequestsTable = new MatchingRequestsTable(memDbDataSource);
      matchingRequestsTable.createTableIfNotExists().createIndexesIfNotExists();
    }
    {
      GameStatesTable gameStatesTable = new GameStatesTable(fileDbDataSource);
      gameStatesTable.createTableIfNotExists().createIndexesIfNotExists();
      gameStatesTable.trimAndBackupToFile(factory.getDatabaseDirectory(),
          Const.TRIM_THRESHOLD_OF_GAME_STATE_TABLE);

      GameStatesTable gameStatesTableInMem = new GameStatesTable(memDbDataSource);
      gameStatesTableInMem.createTableIfNotExists().createIndexesIfNotExists();
      gameStatesTableInMem.insert(gameStatesTable.selectAll().toArray(GameState[]::new));

      this.gameStatesTables = new GameStatesTables(gameStatesTable, gameStatesTableInMem);
    }
    {
      this.votesTable = new VotesTable(memDbDataSource);
      votesTable.createTableIfNotExists().createIndexesIfNotExists();
    }
    this.webSocketManager = new WebsocketSessionsManager(gameStatesTables, problemsTable,
        usersTable, handsUpTable, matchingRequestsTable, memDbDataSource);

    ScheduledExecutorService srv = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread t = Executors.defaultThreadFactory().newThread(runnable);
      t.setDaemon(true);
      return t;
    });
    srv.scheduleWithFixedDelay(Try.createRunnable(() -> {
      Set<String> uids = matchingRequestsTable.createPairOfUsers(gameStatesTables);
      webSocketManager.sendUpdateWaitingRequestStatus(uids);
    }, e -> log.error(e)), Const.INTERVAL_IN_WAITING_ROOM, Const.INTERVAL_IN_WAITING_ROOM,
        TimeUnit.SECONDS);

    // this.app = Javalin.create(config -> {
    // config.addStaticFiles(staticFiles -> {
    // staticFiles.directory = WEBROOT_DIR_NAME;
    // staticFiles.location = Location.CLASSPATH;
    // staticFiles.precompress = false;
    // // staticFiles.precompress = true;
    // });
    // config.autogenerateEtags = true;
    // config.enableCorsForAllOrigins();
    // config.enableWebjars();
    // config.accessManager(new GoAccessManager(usersTable));
    // });

    this.app = Javalin.create(config -> {
      config.staticFiles.add(Const.WEBROOT_DIR_NAME, Location.CLASSPATH);
      config.staticFiles.enableWebjars();
      config.http.generateEtags = true;
      config.plugins.enableCors(cors -> cors.add(corsConfig -> corsConfig.anyHost()));
      config.accessManager(new GoAccessManager(usersTable));
    });

    prepareWebSocket();
    prepareJsonRpc();
    prepareGetHandler();
  }

  private FileDatabaseConfigJson getFileDbConfig() {
    try {
      return getDefaultJacksonMapper().toObject(ResourceUtils.getResourceAsFile("/conf/h2.json"),
          FileDatabaseConfigJson.Builder.class).build();
    } catch (Exception e) {
      log.warn("Try to load h2.json.default");
      return getDefaultJacksonMapper()
          .toObject(ResourceUtils.getResourceAsFile("/conf/h2.json.default"),
              FileDatabaseConfigJson.Builder.class)
          .build();
    }
  }

  public static String getJdbcUrlOfInMemoryDb(String dbName) {
    return "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
  }

  private void prepareWebSocket() {
    app.ws("/websocket/play/checkcon", ws -> ws
        .onConnect(ctx -> log.trace("{}", ctx.session.getUpgradeRequest().getRequestURI())));
    app.ws("/websocket/play", ws -> {
      ws.onConnect(ctx -> {
        webSocketManager.onConnect(ctx.session, ctx.queryParam("userId"), ctx.queryParam("gameId"));
        ctx.enableAutomaticPings(Const.WS_PING_INTERVAL_SEC, TimeUnit.SECONDS);
      });
      ws.onClose(ctx -> webSocketManager.onClose(ctx.session, ctx.status(), ctx.reason()));
      ws.onError(ctx -> webSocketManager.onError(ctx.session, ctx.error()));
      ws.onMessage(ctx -> webSocketManager.onMessage(ctx.queryParam("gameId"), ctx));
    });
  }

  private void prepareJsonRpc() {

    prepareFirebase();

    GoJsonRpcService go = new GoJsonRpcService(webSocketManager, gameStatesTables, problemsTable,
        usersTable, loginsTable, matchingRequestsTable, votesTable, handsUpTable, gameRecordsTable);
    JavalinJsonRpcService srv = new JavalinJsonRpcService(GoApplication.getDefaultJacksonMapper());

    app.post("/app/json/GoJsonRpcService", ctx -> srv.handle(ctx, go));

    app.post("/app/json/AuthRpcService", ctx -> srv.handle(ctx,
        new AuthService(usersTable, loginsTable, passwordsTable, ctx.req())));
  }

  private boolean prepareFirebase() {
    try {
      String url = Files
          .readAllLines(ResourceUtils.getResourceAsFile("/conf/firebase-url.conf").toPath()).get(0);
      AuthService.initialize(url, ResourceUtils.getResourceAsFile("/conf/firebase.json"));
      return true;
    } catch (Exception e) {
      log.warn("Skip firebase settings");
      return false;
    }
  }

  private void prepareGetHandler() {
    class GoHandler implements Handler {

      private final Function<Context, Function<String, Function<ViewModel.Builder, Consumer<UserSession>>>> handler;

      public GoHandler(
          Function<Context, Function<String, Function<ViewModel.Builder, Consumer<UserSession>>>> handler) {
        this.handler = handler;
      }

      @Override
      public void handle(Context ctx) throws Exception {
        String filePath = UrlUtils.of(ctx.url()).getPath().replaceFirst("^/app/", "");
        ViewModel.Builder model = createDefaultModel(usersTable, ctx.req());
        UserSession session = UserSession.wrap(ctx.req().getSession());
        handler.apply(ctx).apply(filePath).apply(model).accept(session);
      }
    }

    app.get("/app", ctx -> ctx.redirect("/app/index.html"));

    app.get("/app/index.html", new GoHandler(ctx -> filePath -> model -> session -> {
      session.getUserId().ifPresent(uid -> {
        boolean attend = loginsTable.isAttendance(uid);
        model.put("isAttendance", attend);
        model.put("problemGroupsJson", problemsTable.getProblemGroupsNode());
      });
      ctx.render(filePath, model.build());
    }));

    app.get("/app/play.html", new GoHandler(ctx -> filePath -> model -> session -> {
      session.getUserId().ifPresent(uid -> {
        boolean attend = loginsTable.isAttendance(uid);
        model.put("isAttendance", attend);
        model.put("problemGroupsJson", problemsTable.getProblemGroupsNode());
      });
      ctx.render(filePath, model.build());
    }), UserRole.LOGIN_ROLES);

    app.get("/app/players-all.html", new GoHandler(ctx -> filePath -> model -> session -> {
      List<Tuple2<User, Login>> users = usersTable.readAllWithLastLogin();
      List<LoginJson> loginJsons =
          users.stream().map(t -> new LoginJson(t.getT2(), t.getT1())).collect(Collectors.toList());
      model.put("userAccounts", loginJsons);
      ctx.render("players.html", model.build());
    }), UserRole.ADMIN);

    app.get("/app/players.html", new GoHandler(ctx -> filePath -> model -> session -> {
      List<Tuple2<User, Login>> users = usersTable.readAllWithLastLogin();
      List<LoginJson> loginJsons = users.stream().filter(t -> t.getT1().isStudent())
          .map(t -> new LoginJson(t.getT2(), t.getT1())).collect(Collectors.toList());
      model.put("userAccounts", loginJsons);
      ctx.render(filePath, model.build());
    }), UserRole.ADMIN);

    app.get("/app/games-all.html", new GoHandler(ctx -> filePath -> model -> session -> {
      List<GameStateViewJson> tmp = gameStatesTables.readTodayGameJsons().stream().map(gsj -> {
        String gameId = gsj.gameId();
        GameStateViewJson json = new GameStateViewJson(gsj, handsUpTable.selectByPrimaryKey(gameId),
            webSocketManager.getWatchingUniqueStudentsNum(gameId));
        return json;
      }).collect(Collectors.toList());
      model.put("games", tmp);
      ctx.render("games.html", model.build());
    }), UserRole.ADMIN);

    app.get("/app/games.html", new GoHandler(ctx -> filePath -> model -> session -> {
      List<String> gids = webSocketManager.readActiveGameIdsOrderByGameId();
      List<GameStateViewJson> tmp =
          gids.stream().map(gid -> gameStatesTables.readLatestGameState(gid))
              .map(gsj -> new GameStateViewJson(gsj, handsUpTable.selectByPrimaryKey(gsj.gameId()),
                  webSocketManager.getWatchingUniqueStudentsNum(gsj.gameId())))
              .collect(Collectors.toList());
      model.put("games",
          tmp.stream().filter(j -> j.watchingStudentsNum() > 0).collect(Collectors.toList()));
      ctx.render(filePath, model.build());
    }), UserRole.ADMIN);

    app.get("/app/fragment/game-record-table.html",
        new GoHandler(ctx -> filePath -> model -> session -> {
          String userId = ctx.queryParam("userId");
          List<GameRecord> records = gameRecordsTable.readByUserId(userId);
          model.put("records", records);
          ctx.render("fragment/game-record-table.html", model.build());
        }), UserRole.ADMIN);

    app.get("/app/fragment/question-table*", new GoHandler(ctx -> filePath -> model -> session -> {
      List<String> gids = handsUpTable.readAllGameIds();
      List<GameStateViewJson> tmp = gameStatesTables.readLatestBoardsJson(gids).stream()
          .map(gsj -> new GameStateViewJson(gsj, handsUpTable.selectByPrimaryKey(gsj.gameId()), 0))
          .toList();
      model.put("games", tmp);
      ctx.render(filePath, model.build());
    }), UserRole.ADMIN);

    app.get("/app/fragment/waiting-request-table.html",
        new GoHandler(ctx -> filePath -> model -> session -> {
          String userId = ctx.queryParam("userId");
          if (userId != null) {
            List<MatchingRequest> tmp = matchingRequestsTable.readRequests();
            model.put("requests",
                tmp.stream().filter(r -> r.userId().equals(userId)).collect(Collectors.toList()));
          } else {
            model.put("requests", matchingRequestsTable.readRequests());
          }
          ctx.render(filePath, model.build());
        }), UserRole.ADMIN);

    app.get("/app/fragment/waiting-request-table-small.html",
        new GoHandler(ctx -> filePath -> model -> session -> {
          String userId = ctx.queryParam("userId");
          List<MatchingRequest> tmp = matchingRequestsTable.readRequests();
          MatchingRequest req = tmp.stream().filter(r -> r.userId().equals(userId)).findAny()
              .orElse(new MatchingRequest());
          model.put("req", req);
          model.put("reqNum", tmp.size());
          ctx.render(filePath, model.build());
        }), UserRole.ADMIN);

    app.get("/app/*", new GoHandler(ctx -> filePath -> model -> session -> {
      ctx.render(filePath, model.build());
    }));

  }



  private ViewModel.Builder createDefaultModel(UsersTable usersTable, HttpServletRequest request) {
    ViewModel.Builder modelBuilder = ViewModel.builder();
    modelBuilder.put("currentUser", getCurrentUserAccount(usersTable, request));
    modelBuilder.put("webjars", WEB_JARS_VERSIONS);
    modelBuilder.setFileModifiedDate(Const.WEBROOT_DIR, 10, "js", "css");
    return modelBuilder;
  }

  private User getCurrentUserAccount(UsersTable usersTable, HttpServletRequest request) {
    Optional<User> u = UserSession.wrap(request.getSession()).getUserId()
        .map(uid -> usersTable.selectByPrimaryKey(uid));
    return u.orElse(new User());
  }

  public static record LoginJson(Login login, User user) {

  }

  public static JacksonMapper getDefaultJacksonMapper() {
    return JacksonMapper.getIgnoreUnknownPropertiesMapper();
  }

  public static record GameStateViewJson(GameState gameState, HandUp handUp,
      int watchingStudentsNum) {

  }

}
