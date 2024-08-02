package org.nkjmlab.go.javalin;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.nkjmlab.go.javalin.handler.FirebaseConfigs;
import org.nkjmlab.go.javalin.handler.FirebaseConfigs.FirebaseConfig;
import org.nkjmlab.go.javalin.handler.GoGetHandlers;
import org.nkjmlab.go.javalin.jsonrpc.AuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoAuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService;
import org.nkjmlab.go.javalin.model.relation.GoTables;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.util.firebase.auth.BasicFirebaseAuthHandler;
import org.nkjmlab.util.firebase.auth.FirebaseAuthHandler;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.function.Try;
import org.nkjmlab.util.java.lang.JavaSystemProperties;
import org.nkjmlab.util.java.lang.ProcessUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;
import org.nkjmlab.util.java.web.WebApplicationFileLocation;
import org.nkjmlab.util.javalin.JsonRpcJavalinService;
import org.nkjmlab.util.thymeleaf.TemplateEngineBuilder;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;

public class GoApplication {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private final Javalin app;
  private final int port;
  private final boolean useCache = false;
  private final boolean usePopupSignin = true;

  public static void main(String[] args) {

    int port = 4567;
    log.info("start (port:{}) => {}", port, JavaSystemProperties.create());

    ProcessUtils.stopProcessBindingPortIfExists(port);

    new GoApplication(
            new GoDataSourceManager(ResourceUtils.getResourceAsFile("/conf/h2.json")), port)
        .start();
  }

  void start() {
    app.start(port);
  }

  public GoApplication(GoDataSourceManager basicDataSource, int port) {
    this.port = port;

    log.info(
        "log4j2.configurationFile={}, Logger level={}",
        System.getProperty("log4j2.configurationFile"),
        log.getLevel());

    WebApplicationFileLocation webAppConfig = WebApplicationFileLocation.builder().build();

    this.app =
        Javalin.create(
            config -> {
              config.staticFiles.add(webAppConfig.webRootDirectory().toString(), Location.EXTERNAL);
              config.staticFiles.enableWebjars();
              config.http.generateEtags = true;
              config.fileRenderer(
                  new JavalinThymeleaf(
                      TemplateEngineBuilder.builder()
                          .setCacheTtlMs(useCache ? 1000 * 10 : 0)
                          .build()));
              config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            });

    GoTables goTables =
        GoTables.prepareTables(
            webAppConfig.webRootDirectory().toFile(),
            webAppConfig.appRootDirectory().toFile(),
            basicDataSource);

    WebsocketSessionsManager webSocketManager =
        new WebsocketSessionsManager(goTables, basicDataSource.createHikariInMemoryDataSource());

    scheduleCheckMatchingRequest(webSocketManager, goTables, 30);

    GoAuthService authService = createAuthService(goTables.usersTable);

    {
      GoAccessManager accessManager = new GoAccessManager(goTables.usersTable, authService);
      app.beforeMatched(ctx -> accessManager.manage(ctx));
    }
    prepareWebSocket(app, webSocketManager, 27);
    prepareJsonRpc(
        app,
        webSocketManager,
        new GoJsonRpcService(webSocketManager, goTables),
        new AuthService.Factory(goTables, authService));

    FirebaseConfig fbConf =
        usePopupSignin
            ? getFileFirebaseJson(ResourceUtils.getResourceAsFile("/conf/firebaseConfigs.json"))
                .firebaseAppConfig()
            : getFileFirebaseJson(ResourceUtils.getResourceAsFile("/conf/firebaseConfigs.json"))
                .firebaseProxyConfig();

    new GoGetHandlers(
            app, webSocketManager, webAppConfig, fbConf, goTables, authService, usePopupSignin)
        .prepareGetHandlers();
  }

  private static GoAuthService createAuthService(UsersTable usersTable) {
    FirebaseAuthHandler firebaseService =
        BasicFirebaseAuthHandler.create(
            usersTable.readAll().stream().map(u -> u.email()).toList(),
            ResourceUtils.getResourceAsFile("/conf/googleOAuth.json"));
    return new GoAuthService(usersTable, firebaseService);
  }

  /**
   * @param webSocketManager
   * @param goTables
   * @param matchingIntervalSec このインターバルが小さいと待合室に十分に人数が入っていない状態でマッチングが始まる可能性が高くなってしまう．30秒程度が妥当か．
   */
  private static void scheduleCheckMatchingRequest(
      WebsocketSessionsManager webSocketManager, GoTables goTables, int matchingIntervalSec) {

    ScheduledExecutorService srv =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = Executors.defaultThreadFactory().newThread(runnable);
              t.setDaemon(true);
              return t;
            });

    srv.scheduleWithFixedDelay(
        Try.createRunnable(
            () -> {
              Set<String> uids = goTables.matchingRequestsTable.createPairOfUsers();
              webSocketManager.sendUpdateWaitingRequestStatus(uids);
            },
            e -> log.error(e)),
        0,
        matchingIntervalSec,
        TimeUnit.SECONDS);
  }

  /**
   * @param app
   * @param webSocketManager
   * @param websocketPingIntervalSec WebSocketのpingを送信する間隔. 30秒程度が妥当か．
   */
  private static void prepareWebSocket(
      Javalin app, WebsocketSessionsManager webSocketManager, int websocketPingIntervalSec) {
    app.ws(
        "/websocket/play/checkcon",
        ws ->
            ws.onConnect(ctx -> log.trace("{}", ctx.session.getUpgradeRequest().getRequestURI())));
    app.ws(
        "/websocket/play",
        ws -> {
          ws.onConnect(
              ctx -> {
                webSocketManager.onConnect(
                    ctx.session, ctx.queryParam("userId"), ctx.queryParam("gameId"));
                ctx.enableAutomaticPings(websocketPingIntervalSec, TimeUnit.SECONDS);
              });
          ws.onClose(ctx -> webSocketManager.onClose(ctx.session, ctx.status(), ctx.reason()));
          ws.onError(ctx -> webSocketManager.onError(ctx.session, ctx.error()));
          ws.onMessage(ctx -> webSocketManager.onMessage(ctx.queryParam("gameId"), ctx));
        });
  }

  private static void prepareJsonRpc(
      Javalin app,
      WebsocketSessionsManager webSocketManager,
      GoJsonRpcService jsonRpcSrv,
      AuthService.Factory authServiceFactory) {

    JsonRpcJavalinService srv = new JsonRpcJavalinService(GoApplication.getDefaultJacksonMapper());
    app.post("/app/json/GoJsonRpcService", ctx -> srv.handle(ctx, jsonRpcSrv));
    app.post(
        "/app/json/AuthRpcService", ctx -> srv.handle(ctx, authServiceFactory.create(ctx.req())));
  }

  public static JacksonMapper getDefaultJacksonMapper() {
    return JacksonMapper.getIgnoreUnknownPropertiesMapper();
  }

  private static FirebaseConfigs getFileFirebaseJson(File json) {
    try {
      return JacksonMapper.getDefaultMapper().toObject(json, FirebaseConfigs.class);
    } catch (Exception e) {
      return null;
    }
  }
}
