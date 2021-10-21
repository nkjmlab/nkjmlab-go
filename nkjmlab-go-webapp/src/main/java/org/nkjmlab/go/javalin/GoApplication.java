package org.nkjmlab.go.javalin;

import java.io.File;
import java.util.List;
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
import org.nkjmlab.go.javalin.model.relation.ProblemsTable;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.relation.VotesTable;
import org.nkjmlab.go.javalin.model.row.GameRecord;
import org.nkjmlab.go.javalin.model.row.GameState;
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.go.javalin.websocket.WebsoketSessionsTable;
import org.nkjmlab.sorm4j.internal.util.StringUtils;
import org.nkjmlab.sorm4j.internal.util.Try;
import org.nkjmlab.sorm4j.sql.result.Tuple2;
import org.nkjmlab.util.concurrent.ForkJoinPoolUtils;
import org.nkjmlab.util.db.h2.FileDatabaseConfig;
import org.nkjmlab.util.db.h2.H2Server;
import org.nkjmlab.util.io.FileUtils;
import org.nkjmlab.util.json.JacksonMapper;
import org.nkjmlab.util.lang.ProcessUtils;
import org.nkjmlab.util.lang.ResourceUtils;
import org.nkjmlab.util.thymeleaf.TemplateEngineBuilder;
import org.nkjmlab.util.websrv.UserSession;
import org.nkjmlab.util.websrv.ViewModel;
import org.nkjmlab.util.webui.jsonrpc.JsonRpcRequest;
import org.nkjmlab.util.webui.jsonrpc.JsonRpcResponse;
import org.nkjmlab.util.webui.jsonrpc.JsonRpcService;
import org.nkjmlab.util.webui.jsonrpc.JsonRpcUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;

public class GoApplication {

  private static org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private static final File APP_ROOT_DIR = ResourceUtils.getFile("/");
  private static final String WEBROOT_DIR_NAME = "/webroot";
  private static final File WEBROOT_DIR = new File(APP_ROOT_DIR, WEBROOT_DIR_NAME);
  private static final File USER_HOME_DIR = FileUtils.getUserDirectory();
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
    H2Server.startAndWait();
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
    FileDatabaseConfig fileDbConf = JacksonMapper.getDefaultMapper()
        .toObject(ResourceUtils.getFile("/h2.conf"), FileDatabaseConfig.Builder.class).build();

    this.memDbDataSource = createH2DataSource(getJdbcUrlOfInMemoryDb(fileDbConf.getDatabaseName()),
        fileDbConf.getUsername(), fileDbConf.getPassword());
    this.fileDbDataSource = createHikariDataSource(fileDbConf.getJdbcUrl(),
        fileDbConf.getUsername(), fileDbConf.getPassword());
    // H2Server.openBrowser(memDbDataSource, true);


    prepareJavalin();
    prepareTable(fileDbConf);
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

  private void prepareTable(FileDatabaseConfig fileDbConf) {
    this.problemsTable = new ProblemsTable(memDbDataSource);
    problemsTable.dropAndInsertInitialProblemsToTable(PROBLEM_DIR);

    this.loginsTable = new LoginsTable(fileDbDataSource);

    this.handsUpTable = new HandsUpTable(memDbDataSource);
    handsUpTable.dropTableIfExists();
    handsUpTable.createTableAndIndexesIfNotExists();

    this.usersTable = new UsersTable(fileDbDataSource);
    usersTable.readFromFileAndMerge(ResourceUtils.getFile("/users.csv"));

    this.gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    gameRecordsTable.recalculateAndUpdateRank(usersTable);

    this.matchingRequestsTable = new MatchingRequestsTable(memDbDataSource);

    this.gameStatesTable = new GameStatesTable(fileDbDataSource);
    gameStatesTable.trimAndBackupToFile(fileDbConf.getDatabaseDirectory(),
        TRIM_THRESHOLD_OF_GAME_STATE_TABLE);

    this.gameStatesTableInMem = new GameStatesTable(memDbDataSource);
    gameStatesTableInMem.insert(gameStatesTable.readAll().toArray(GameState[]::new));

    this.gameStatesTables = new GameStatesTables(fileDbDataSource, memDbDataSource);

    this.votesTable = new VotesTable(memDbDataSource);

    this.gameRecordsTable = new GameRecordsTable(fileDbDataSource);
    this.gameRecordsTable.backupToCsv(BACKUP_DIR);


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

    AuthService.initialize("https://toho-go-fb.firebaseio.com",
        ResourceUtils.getFile("/firebase.json"));

    final GoJsonRpcService goJsonRpcService = new GoJsonRpcService(wsManager, gameStatesTables,
        problemsTable, usersTable, loginsTable, matchingRequestsTable, votesTable, handsUpTable,
        websoketSessionsTable, gameRecordsTable);


    JacksonMapper mapper = JacksonMapper.getDefaultMapper();
    JsonRpcService jsonRpcService = new JsonRpcService(mapper);

    app.post("/app/json/GoJsonRpcService", ctx -> {
      JsonRpcRequest jreq = JsonRpcUtils.toJsonRpcRequest(mapper, ctx.req);
      Object srv = AuthServiceInterface.getDeclaredMethodNames().contains(jreq.getMethod())
          ? new AuthService(usersTable, loginsTable, ctx.req)
          : goJsonRpcService;
      JsonRpcResponse jres = jsonRpcService.callHttpJsonRpc(srv, jreq, ctx.res);
      String ret = mapper.toJson(jres);
      ctx.result(ret).contentType("application/json");
    });
  }


  private void prepareGetHandler() {
    app.get("/app", ctx -> ctx.redirect("/app/index.html"));

    app.get("/app/{pageName}", ctx -> {
      String pageName =
          ctx.pathParam("pageName") == null ? "index.html" : ctx.pathParam("pageName");
      ViewModel model = createDefaultModel(usersTable, ctx.req);
      switch (pageName) {
        case "play.html": {
          UserSession session = UserSession.wrap(ctx.req.getSession());
          if (!session.isLogined()) {
            model.put("requireToLogin", true);
            break;
          }
          boolean attend = loginsTable.isAttendance(session.getUserId());
          model.put("isAttendance", attend);
          model.put("problemGroupsJson", problemsTable.getproblemGroupsNode());
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
      ctx.render(pageName, model);
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



  private ViewModel createDefaultModel(UsersTable usersTable, HttpServletRequest request) {
    ViewModel model =
        new ViewModel.Builder().setFileModifiedDate(WEBROOT_DIR, true, "js", "css").build();
    model.put("currentUser", getCurrentUserAccount(usersTable, request));
    return model;
  }



  private User getCurrentUserAccount(UsersTable usersTable, HttpServletRequest request) {
    User u = usersTable.readByPrimaryKey(UserSession.wrap(request.getSession()).getUserId());
    return u == null ? new User() : u;
  }

  private void isAdminOrThrow(UsersTable usersTable, HttpServletRequest req) {
    FirebaseUserSession session = FirebaseUserSession.wrap(req.getSession());
    if (!session.isSigninFirebase()) {
      throw new RuntimeException("Should be login with firebase");
    }
    String email = session.getEmail();
    User u = usersTable.readByEmail(email);
    if (u == null) {
      throw new RuntimeException(StringUtils.format("User not found {}", email));
    }
    if (!u.isAdmin()) {
      throw new RuntimeException(StringUtils.format("User is not admin {}", email));
    }

  }



}
