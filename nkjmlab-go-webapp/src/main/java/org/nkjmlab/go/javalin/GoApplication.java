package org.nkjmlab.go.javalin;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.nkjmlab.go.javalin.jsonrpc.AuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProcess;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProperties;
import org.nkjmlab.util.firebase.auth.BasicFirebaseAuthHandler;
import org.nkjmlab.util.firebase.auth.FirebaseAuthHandler;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.function.Try;
import org.nkjmlab.util.java.lang.JavaSystemProperties;
import org.nkjmlab.util.java.lang.ProcessUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;
import org.nkjmlab.util.java.web.WebApplicationConfig;
import org.nkjmlab.util.javalin.JsonRpcJavalinService;
import org.nkjmlab.util.thymeleaf.ThymeleafTemplateEngineBuilder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;

public class GoApplication {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final Javalin app;

  private static final WebApplicationConfig WEB_APP_CONFIG = WebApplicationConfig.builder()
      .addWebJar("jquery", "sweetalert2", "bootstrap", "bootstrap-treeview", "clipboard",
          "fortawesome__fontawesome-free", "stacktrace-js", "datatables", "firebase", "firebaseui",
          "ua-parser-js", "blueimp-load-image", "emojionearea")
      .build();

  public static void main(String[] args) {

    int port = 4567;
    log.info("start (port:{}) => {}", port, JavaSystemProperties.create());

    ProcessUtils.stopProcessBindingPortIfExists(port);
    new H2TcpServerProcess(H2TcpServerProperties.builder().build()).awaitStart();

    new GoApplication().start(port);
  }

  private void start(int port) {
    app.start(port);
  }

  public GoApplication() {

    final long THYMELEAF_EXPIRE_TIME_MILLI_SECOND = 1 * 1000;

    log.info("log4j2.configurationFile={}, Logger level={}",
        System.getProperty("log4j2.configurationFile"), log.getLevel());


    DataSourceManager basicDataSource = new DataSourceManager();

    GoTables goTables = GoTables.prepareTables(WEB_APP_CONFIG.getWebRootDirectory().toFile(),
        WEB_APP_CONFIG.getAppRootDirectory().toFile(), basicDataSource);

    WebsocketSessionsManager webSocketManager =
        new WebsocketSessionsManager(goTables, basicDataSource.createHikariInMemoryDataSource());

    scheduleCheckMatchingRequest(webSocketManager, goTables);


    FirebaseAuthHandler firebaseService = BasicFirebaseAuthHandler.create(
        goTables.usersTable.readAll().stream().map(u -> u.email()).toList(),
        ResourceUtils.getResourceAsFile("/conf/firebase.json"));
    GoAuthService authService = new GoAuthService(goTables.usersTable, firebaseService);


    this.app = Javalin.create(config -> {
      config.staticFiles.add(WEB_APP_CONFIG.getWebRootDirectory().toFile().getPath(),
          Location.EXTERNAL);
      config.staticFiles.enableWebjars();
      config.http.generateEtags = true;
      config.fileRenderer(new JavalinThymeleaf(ThymeleafTemplateEngineBuilder.builder()
          .setTtlMs(THYMELEAF_EXPIRE_TIME_MILLI_SECOND).build()));
      config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));

    });
    GoAccessManager accessManager = new GoAccessManager(goTables.usersTable, authService);
    app.beforeMatched(ctx -> accessManager.manage(ctx));

    prepareWebSocket(app, webSocketManager);
    prepareJsonRpc(app, webSocketManager, new GoJsonRpcService(webSocketManager, goTables),
        new AuthService.Factory(goTables, authService));


    new GoGetHandlers(app, webSocketManager, WEB_APP_CONFIG, goTables, authService)
        .prepareGetHandlers();
  }

  private static void scheduleCheckMatchingRequest(WebsocketSessionsManager webSocketManager,
      GoTables goTables) {
    // このインターバルが小さいと待合室に十分に人数が入っていない状態でマッチングが始まる可能性が高くなってしまう．30秒が妥当か．
    final int MATCHING_INTERVAL_SEC = 30;

    ScheduledExecutorService srv = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread t = Executors.defaultThreadFactory().newThread(runnable);
      t.setDaemon(true);
      return t;
    });

    srv.scheduleWithFixedDelay(Try.createRunnable(() -> {
      Set<String> uids =
          goTables.matchingRequestsTable.createPairOfUsers(goTables.gameStatesTables);
      webSocketManager.sendUpdateWaitingRequestStatus(uids);
    }, e -> log.error(e)), 0, MATCHING_INTERVAL_SEC, TimeUnit.SECONDS);

  }


  private static void prepareWebSocket(Javalin app, WebsocketSessionsManager webSocketManager) {
    final int WS_PING_INTERVAL_SEC = 27;
    app.ws("/websocket/play/checkcon", ws -> ws
        .onConnect(ctx -> log.trace("{}", ctx.session.getUpgradeRequest().getRequestURI())));
    app.ws("/websocket/play", ws -> {
      ws.onConnect(ctx -> {
        webSocketManager.onConnect(ctx.session, ctx.queryParam("userId"), ctx.queryParam("gameId"));
        ctx.enableAutomaticPings(WS_PING_INTERVAL_SEC, TimeUnit.SECONDS);
      });
      ws.onClose(ctx -> webSocketManager.onClose(ctx.session, ctx.status(), ctx.reason()));
      ws.onError(ctx -> webSocketManager.onError(ctx.session, ctx.error()));
      ws.onMessage(ctx -> webSocketManager.onMessage(ctx.queryParam("gameId"), ctx));
    });
  }

  private static void prepareJsonRpc(Javalin app, WebsocketSessionsManager webSocketManager,
      GoJsonRpcService jsonRpcSrv, AuthService.Factory authServiceFactory) {

    JsonRpcJavalinService srv = new JsonRpcJavalinService(GoApplication.getDefaultJacksonMapper());
    app.post("/app/json/GoJsonRpcService", ctx -> srv.handle(ctx, jsonRpcSrv));
    app.post("/app/json/AuthRpcService",
        ctx -> srv.handle(ctx, authServiceFactory.create(ctx.req())));
  }


  public static JacksonMapper getDefaultJacksonMapper() {
    return JacksonMapper.getIgnoreUnknownPropertiesMapper();
  }
}
