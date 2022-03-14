package org.nkjmlab.go.javalin;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.nkjmlab.go.javalin.fbauth.AuthService;
import org.nkjmlab.go.javalin.fbauth.AuthServiceInterface;
import org.nkjmlab.go.javalin.fbauth.FirebaseUserSession;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService;
import org.nkjmlab.go.javalin.model.json.GameStateViewJson;
import org.nkjmlab.go.javalin.model.json.LoginJson;
import org.nkjmlab.go.javalin.model.json.MatchingRequestJson;
import org.nkjmlab.go.javalin.model.relation.GameRecordsTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTable;
import org.nkjmlab.go.javalin.model.relation.GameStatesTables;
import org.nkjmlab.go.javalin.model.relation.HandsUpTable;
import org.nkjmlab.go.javalin.model.relation.LoginsTable;
import org.nkjmlab.go.javalin.model.relation.MatchingRequestsTable;
import org.nkjmlab.go.javalin.model.relation.PasswordsTable;
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
import org.nkjmlab.go.javalin.model.row.GameRecord;
import org.nkjmlab.go.javalin.model.row.GameState;
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.go.javalin.websocket.WebsoketSessionsTable;
import org.nkjmlab.sorm4j.common.Tuple.Tuple2;
import org.nkjmlab.sorm4j.internal.util.ParameterizedStringUtils;
import org.nkjmlab.sorm4j.internal.util.Try;
import org.nkjmlab.util.h2.H2LocalDataSourceFactory;
import org.nkjmlab.util.h2.H2ServerUtils;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.concurrent.ForkJoinPoolUtils;
import org.nkjmlab.util.java.io.SystemFileUtils;
import org.nkjmlab.util.java.json.FileDatabaseConfigJson;
import org.nkjmlab.util.java.lang.ProcessUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;
import org.nkjmlab.util.javax.servlet.JsonRpcService;
import org.nkjmlab.util.javax.servlet.UserSession;
import org.nkjmlab.util.javax.servlet.ViewModel;
import org.nkjmlab.util.javax.servlet.ViewModel.Builder;
import org.nkjmlab.util.jsonrpc.JsonRpcRequest;
import org.nkjmlab.util.jsonrpc.JsonRpcResponse;
import org.nkjmlab.util.thymeleaf.TemplateEngineBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;

public class GoApplication {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private static final File APP_ROOT_DIR = ResourceUtils.getResourceAsFile("/");
  private static final String WEBROOT_DIR_NAME = "/webroot";
  private static final File WEBROOT_DIR = new File(APP_ROOT_DIR, WEBROOT_DIR_NAME);
  private static final File USER_HOME_DIR = SystemFileUtils.getUserHomeDirectory();
  public static final File BACKUP_DIR = new File(USER_HOME_DIR, "go-bkup/");
  public static final File PROBLEM_DIR = new File(APP_ROOT_DIR, "problem");
  public static final File PROBLEM_BACKUP_DIR = new File(APP_ROOT_DIR, "problem-auto-bkup");

  public static final File CURRENT_ICON_DIR = new File(WEBROOT_DIR, "img/icon");
  public static final File UPLOADED_ICON_DIR = new File(WEBROOT_DIR, "img/icon-uploaded");
  public static final File RANDOM_ICON_DIR = new File(WEBROOT_DIR, "img/icon-random");
  public static final File INITIAL_ICON_DIR = new File(WEBROOT_DIR, "img/icon-initial");

  private static long THYMELEAF_EXPIRE_TIME_MILLI_SECOND = 1 * 1000;

  private static int TRIM_THRESHOLD_OF_GAME_STATE_TABLE = 30000;

  private DataSource memDbDataSource;
  private DataSource fileDbDataSource;

  private Javalin app;
  private ProblemsTable problemsTable;
  private HandsUpTable handsUpTable;
  private UsersTable usersTable;
  private PasswordsTable passwordsTable;
  private MatchingRequestsTable matchingRequestsTable;
  private GameStatesTable gameStatesTable;
  private GameStatesTable gameStatesTableInMem;
  private GameStatesTables gameStatesTables;
  private VotesTable votesTable;
  private WebsoketSessionsTable websoketSessionsTable;
  private GameRecordsTable gameRecordsTable;
  private LoginsTable loginsTable;
  private WebsocketSessionsManager wsManager;



  static {
    H2ServerUtils.startDefaultTcpServerProcessAndWaitFor();
    H2ServerUtils.startDefaultWebConsoleServerProcessAndWaitFor();
  }

  public static void main(String[] args) {
    if (args.length != 0) {
      THYMELEAF_EXPIRE_TIME_MILLI_SECOND = Long.valueOf(args[0]);
    }
    int port = 4567;
    ProcessUtils.stopProcessBindingPortIfExists(port);

    new GoApplication().start(port);
  }

  private void start(int port) {
    app.start(port);
    log.info("start");

  }

  public GoApplication() {
    FileDatabaseConfigJson fileDbConf;
    try {
      fileDbConf = JacksonMapper.getDefaultMapper()
          .toObject(ResourceUtils.getResourceAsFile("/conf/h2.json"), FileDatabaseConfigJson.Builder.class)
          .build();
    } catch (Exception e) {
      log.warn("Try to load h2.json.default");
      fileDbConf =
          JacksonMapper.getDefaultMapper().toObject(ResourceUtils.getResourceAsFile("/conf/h2.json.default"),
              FileDatabaseConfigJson.Builder.class).build();
    }

    H2LocalDataSourceFactory factory =
        H2LocalDataSourceFactory.builder(fileDbConf.databaseDirectory, fileDbConf.databaseName,
            fileDbConf.username, fileDbConf.password).build();

    this.memDbDataSource = createH2DataSource(factory.getInMemoryModeJdbcUrl(),
        factory.getUsername(), factory.getPassword());
    this.fileDbDataSource = createHikariDataSource(factory.getServerModeJdbcUrl(),
        factory.getUsername(), factory.getPassword());
    // H2Server.openBrowser(memDbDataSource, true);


    prepareJavalin();
    prepareTable(factory);
    prepareWebSocket();
    prepareJsonRpc();
    prepareGetHandler();
  }


  public static String getJdbcUrlOfInMemoryDb(String dbName) {
    return "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1";
  }



  private void prepareJavalin() {
    JavalinThymeleaf.configure(new TemplateEngineBuilder().setPrefix("/templates/")
        .setTtlMs(THYMELEAF_EXPIRE_TIME_MILLI_SECOND).build());

    this.app = Javalin.create(config -> {
      config.addStaticFiles(WEBROOT_DIR_NAME, Location.CLASSPATH);
      config.autogenerateEtags = true;
      // config.precompressStaticFiles = true;
      config.enableCorsForAllOrigins();
    });
  }

  private void prepareTable(H2LocalDataSourceFactory dsFactory) {
    this.problemsTable = new ProblemsTable(memDbDataSource);
    problemsTable.dropAndInsertInitialProblemsToTable(PROBLEM_DIR);

    this.loginsTable = new LoginsTable(fileDbDataSource);
    loginsTable.writeCsv(new File(BACKUP_DIR, "logins-" + System.currentTimeMillis() + ".csv"));



    this.handsUpTable = new HandsUpTable(memDbDataSource);
    handsUpTable.createTableAndIndexesIfNotExists();

    this.usersTable = new UsersTable(fileDbDataSource);
    usersTable.dropTableIfExists();
    usersTable.createTableAndIndexesIfNotExists();
    try {
      File f = ResourceUtils.getResourceAsFile("/conf/users.csv");
      usersTable.readFromFileAndMerge(f);
    } catch (Exception e) {
      log.warn("load users.csv.default ...");
      File f = ResourceUtils.getResourceAsFile("/conf/users.csv.default");
      usersTable.readFromFileAndMerge(f);
    }


    this.passwordsTable = new PasswordsTable(fileDbDataSource);

    try {
      File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv");
      passwordsTable.readFromFileAndMerge(f);
    } catch (Exception e) {
      log.warn("load password.csv.default ...");
      File f = ResourceUtils.getResourceAsFile("/conf/passwords.csv.default");
      passwordsTable.readFromFileAndMerge(f);
    }


    this.gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    gameRecordsTable.recalculateAndUpdateRank(usersTable);

    this.matchingRequestsTable = new MatchingRequestsTable(memDbDataSource);

    this.gameStatesTable = new GameStatesTable(fileDbDataSource);
    gameStatesTable.trimAndBackupToFile(dsFactory.getDatabaseDirectory(),
        TRIM_THRESHOLD_OF_GAME_STATE_TABLE);

    this.gameStatesTableInMem = new GameStatesTable(memDbDataSource);
    gameStatesTableInMem.insert(gameStatesTable.readAll().toArray(GameState[]::new));

    this.gameStatesTables = new GameStatesTables(fileDbDataSource, memDbDataSource);

    this.votesTable = new VotesTable(memDbDataSource);

    this.gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    this.gameRecordsTable
        .writeCsv(new File(BACKUP_DIR, "game-record" + System.currentTimeMillis() + ".csv"));


    this.websoketSessionsTable = new WebsoketSessionsTable(memDbDataSource);
    this.wsManager = new WebsocketSessionsManager(gameStatesTables, problemsTable,
        websoketSessionsTable, usersTable, handsUpTable, matchingRequestsTable);

  }

  private void prepareWebSocket() {
    app.ws("/websocket/play/checkcon", ws -> {
      ws.onConnect(ctx -> {
        log.debug("{}", ctx.session.getUpgradeRequest().getRequestURI());
      });
    });
    app.ws("/websocket/play", ws -> {
      ws.onConnect(ctx -> {
        wsManager.onConnect(ctx.session, ctx.queryParam("userId"), ctx.queryParam("gameId"));
      });
      ws.onClose(ctx -> wsManager.onClose(ctx.session, ctx.status(), ctx.reason()));
      ws.onError(ctx -> wsManager.onError(ctx.session, ctx.error()));
    });


    final int INTERVAL_IN_WAITING_ROOM = 10;

    ScheduledExecutorService srv = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread t = Executors.defaultThreadFactory().newThread(runnable);
      t.setDaemon(true);
      return t;
    });
    srv.scheduleWithFixedDelay(Try.createRunnable(() -> {
      List<String> uids = matchingRequestsTable.createPairOfUsers(gameStatesTables);
      wsManager.sendUpdateWaitingRequestStatus(uids);
    }, e -> log.error(e)), INTERVAL_IN_WAITING_ROOM, INTERVAL_IN_WAITING_ROOM, TimeUnit.SECONDS);
  }

  private void prepareJsonRpc() {

    prepareFirebase();

    final GoJsonRpcService goJsonRpcService = new GoJsonRpcService(wsManager, gameStatesTables,
        problemsTable, usersTable, loginsTable, matchingRequestsTable, votesTable, handsUpTable,
        websoketSessionsTable, gameRecordsTable);


    JacksonMapper mapper = JacksonMapper.getDefaultMapper();
    JsonRpcService jsonRpcService = new JsonRpcService(mapper);

    app.post("/app/json/GoJsonRpcService", ctx -> {
      JsonRpcRequest jreq = jsonRpcService.toJsonRpcRequest(ctx.req);
      Object srv = AuthServiceInterface.getDeclaredMethodNames().contains(jreq.getMethod())
          ? new AuthService(usersTable, loginsTable, passwordsTable, ctx.req)
          : goJsonRpcService;
      JsonRpcResponse jres = jsonRpcService.callHttpJsonRpc(srv, jreq, ctx.res);
      String ret = mapper.toJson(jres);
      ctx.result(ret).contentType("application/json");
    });
  }


  private boolean prepareFirebase() {
    try {
      String url =
          Files.readAllLines(ResourceUtils.getResourceAsFile("/conf/firebase-url.conf").toPath()).get(0);
      AuthService.initialize(url, ResourceUtils.getResourceAsFile("/conf/firebase.json"));
      return true;
    } catch (Exception e) {
      log.warn("Skip firebase settings");
      return false;
    }
  }

  private void prepareGetHandler() {
    app.get("/app", ctx -> ctx.redirect("/app/index.html"));

    app.get("/app/<pageName>", ctx -> {
      String pageName =
          ctx.pathParam("pageName") == null ? "index.html" : ctx.pathParam("pageName");
      Builder model = createDefaultModel(usersTable, ctx.req);
      switch (pageName) {
        case "play.html": {
          UserSession session = UserSession.wrap(ctx.req.getSession());
          if (!session.isLogined()) {
            model.put("requireToLogin", true);
            break;
          }
          session.getUserId().ifPresent(uid -> {
            boolean attend = loginsTable.isAttendance(uid);
            model.put("isAttendance", attend);
            model.put("problemGroupsJson", problemsTable.getproblemGroupsNode());
          });
          break;
        }
        case "players-all.html": {
          try {
            isAdminOrThrow(usersTable, ctx.req);
          } catch (Exception e) {
            ctx.redirect("/app/index.html");
            log.error(e.getMessage());
            return;
          }
          List<Tuple2<User, Login>> users = usersTable.readAllWithLastLogin();
          List<LoginJson> loginJsons = users.stream().map(t -> new LoginJson(t.getT2(), t.getT1()))
              .collect(Collectors.toList());
          model.put("userAccounts", loginJsons);
          pageName = "players.html";
          break;
        }
        case "players.html": {
          try {
            isAdminOrThrow(usersTable, ctx.req);
          } catch (Exception e) {
            ctx.redirect("/app/index.html");
            log.error(e.getMessage());
            return;
          }
          List<Tuple2<User, Login>> users = usersTable.readAllWithLastLogin();
          List<LoginJson> loginJsons = users.stream().filter(t -> t.getT1().isStudent())
              .map(t -> new LoginJson(t.getT2(), t.getT1())).collect(Collectors.toList());
          model.put("userAccounts", loginJsons);
          break;
        }
        case "games-all.html": {
          List<GameStateViewJson> tmp = gameStatesTables.readTodayGameJsons().stream().map(gsj -> {
            GameStateViewJson json = new GameStateViewJson(gsj);
            String gameId = gsj.getGameId();
            json.setHandUp(handsUpTable.readByPrimaryKey(gameId));
            json.setWatchingStudentsNum(
                websoketSessionsTable.getWatchingUniqueStudentsNum(usersTable, gameId));
            return json;
          }).collect(Collectors.toList());
          model.put("games", tmp);
          pageName = "games.html";
          break;
        }
        case "games.html": {
          List<String> gids = websoketSessionsTable.readActiveGameIdsOrderByGameId(usersTable);
          List<GameStateViewJson> tmp =
              gids.stream().map(gid -> gameStatesTables.readLatestGameStateJson(gid)).map(gsj -> {
                GameStateViewJson json = new GameStateViewJson(gsj);
                String gameId = gsj.getGameId();
                json.setHandUp(handsUpTable.readByPrimaryKey(gameId));
                json.setWatchingStudentsNum(
                    websoketSessionsTable.getWatchingUniqueStudentsNum(usersTable, gameId));
                return json;
              }).collect(Collectors.toList());
          model.put("games", tmp.stream().filter(j -> j.getWatchingStudentsNum() > 0)
              .collect(Collectors.toList()));
          break;
        }
        case "fragment/game-record-table.html": {
          String userId = ctx.queryParam("userId");
          List<GameRecord> records = gameRecordsTable.readByUserId(userId);
          model.put("records", records);
          break;
        }
        case "fragment/question-table.html":
        case "fragment/question-table-small.html": {
          List<String> gids = handsUpTable.readAllGameIds();
          List<GameStateViewJson> tmp =
              gameStatesTables.readLatestBoardsJson(gids).stream().map(gsj -> {
                GameStateViewJson json = new GameStateViewJson(gsj);
                String gameId = gsj.getGameId();
                json.setHandUp(handsUpTable.readByPrimaryKey(gameId));
                return json;
              }).collect(Collectors.toList());
          model.put("games", tmp);
          break;
        }
        case "fragment/waiting-request-table.html": {
          String userId = ctx.queryParam("userId");
          if (userId != null) {
            List<MatchingRequestJson> tmp = matchingRequestsTable.readRequests();
            model.put("requests", tmp.stream().filter(r -> r.getUserId().equals(userId))
                .collect(Collectors.toList()));
          } else {
            model.put("requests", matchingRequestsTable.readRequests());
          }
          break;
        }
        case "fragment/waiting-request-table-small.html": {
          String userId = ctx.queryParam("userId");
          List<MatchingRequestJson> tmp = matchingRequestsTable.readRequests();
          model.put("req", tmp.stream().filter(r -> r.getUserId().equals(userId)).findAny()
              .orElseGet(MatchingRequestJson::new));
          model.put("reqNum", tmp.size());
        }
      }
      ctx.render(pageName, model.build().getMap());
    });
  }

  private static final int DEFAULT_MAX_CONNECTIONS =
      Math.min(ForkJoinPoolUtils.availableProcessors() * 2 * 2, 10);
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private JdbcConnectionPool createH2DataSource(String url, String user, String password) {
    JdbcConnectionPool ds = JdbcConnectionPool.create(url, user, password);
    ds.setMaxConnections(DEFAULT_MAX_CONNECTIONS);
    ds.setLoginTimeout(DEFAULT_TIMEOUT_SECONDS);
    return ds;
  }

  public static HikariDataSource createHikariDataSource(String url, String user, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    config.setMaximumPoolSize(DEFAULT_MAX_CONNECTIONS);
    config.setConnectionTimeout(DEFAULT_TIMEOUT_SECONDS * 1000);
    config.addDataSourceProperty("useServerPrepStmts", "true");
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    config.addDataSourceProperty("minimumIdle", "2048");
    return new HikariDataSource(config);
  }



  private Builder createDefaultModel(UsersTable usersTable, HttpServletRequest request) {
    ViewModel.Builder modelBuilder =
        ViewModel.builder().setFileModifiedDate(WEBROOT_DIR, 10, "js", "css");
    modelBuilder.put("currentUser", getCurrentUserAccount(usersTable, request));
    return modelBuilder;
  }



  private User getCurrentUserAccount(UsersTable usersTable, HttpServletRequest request) {
    Optional<User> u = UserSession.wrap(request.getSession()).getUserId()
        .map(uid -> usersTable.readByPrimaryKey(uid));
    return u.orElse(new User());
  }

  private void isAdminOrThrow(UsersTable usersTable, HttpServletRequest req) {
    FirebaseUserSession session = FirebaseUserSession.wrap(req.getSession());
    User u = null;
    if (session.isSigninFirebase()) {
      String email = session.getEmail();
      u = usersTable.readByEmail(email);
    } else if (session.isLogined()) {
      u = session.getUserId().map(userId -> usersTable.readByPrimaryKey(userId)).orElse(null);
    }
    if (u == null) {
      throw new RuntimeException(ParameterizedStringUtils.newString("User not found"));
    }
    if (!u.isAdmin()) {
      throw new RuntimeException(ParameterizedStringUtils.newString("User is not admin"));
    }


  }



}
