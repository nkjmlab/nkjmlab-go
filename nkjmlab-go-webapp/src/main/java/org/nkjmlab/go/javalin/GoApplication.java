package org.nkjmlab.go.javalin;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.auth.AuthService;
import org.nkjmlab.go.javalin.jsonrpc.GoJsonRpcService;
import org.nkjmlab.go.javalin.websocket.WebsocketSessionsManager;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProcess;
import org.nkjmlab.sorm4j.util.h2.server.H2TcpServerProperties;
import org.nkjmlab.util.jackson.JacksonMapper;
import org.nkjmlab.util.java.function.Try;
import org.nkjmlab.util.java.lang.ProcessUtils;
import org.nkjmlab.util.java.lang.ResourceUtils;
import org.nkjmlab.util.java.lang.SystemPropertyUtils;
import org.nkjmlab.util.java.web.WebApplicationConfig;
import org.nkjmlab.util.javalin.JavalinJsonRpcService;
import org.nkjmlab.util.thymeleaf.ThymeleafTemplateEnginBuilder;
import org.thymeleaf.TemplateEngine;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;

public class GoApplication {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


  public static class GoWebAppConfig {
    public static final WebApplicationConfig WEB_APP_CONFIG = WebApplicationConfig.builder()
        .addWebJar("jquery", "sweetalert2", "bootstrap", "bootstrap-treeview", "clipboard",
            "fortawesome__fontawesome-free", "stacktrace-js", "datatables", "firebase",
            "firebaseui", "ua-parser-js", "blueimp-load-image", "emojionearea")
        .build();
    public static final File PROBLEM_DIR =
        new File(GoWebAppConfig.WEB_APP_CONFIG.getAppRootDirectory(), "problem");
    public static final File CURRENT_ICON_DIR =
        new File(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory(), "img/icon");
    public static final File UPLOADED_ICON_DIR =
        new File(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory(), "img/icon-uploaded");
  }

  private final Javalin app;


  public static void main(String[] args) {

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


    final long THYMELEAF_EXPIRE_TIME_MILLI_SECOND = 1 * 1000;
    final int INTERVAL_IN_WAITING_ROOM = 10;

    log.info("log4j2.configurationFile={}, Logger level={}",
        System.getProperty("log4j2.configurationFile"), log.getLevel());


    TemplateEngine engine = ThymeleafTemplateEnginBuilder.builder()
        .setTtlMs(THYMELEAF_EXPIRE_TIME_MILLI_SECOND).build();
    JavalinThymeleaf.init(engine);


    // H2Server.openBrowser(memDbDataSource, true);

    DataSourceManager basicDataSource = new DataSourceManager();
    DataSource memDbDataSource = basicDataSource.createHikariInMemoryDataSource();
    DataSource fileDbDataSource = basicDataSource.createHikariServerModeDataSource();

    GoTables goTables = GoTables.prepareTables(basicDataSource, fileDbDataSource, memDbDataSource);


    WebsocketSessionsManager webSocketManager =
        new WebsocketSessionsManager(goTables, memDbDataSource);

    ScheduledExecutorService srv = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread t = Executors.defaultThreadFactory().newThread(runnable);
      t.setDaemon(true);
      return t;
    });

    srv.scheduleWithFixedDelay(Try.createRunnable(() -> {
      Set<String> uids =
          goTables.matchingRequestsTable.createPairOfUsers(goTables.gameStatesTables);
      webSocketManager.sendUpdateWaitingRequestStatus(uids);
    }, e -> log.error(e)), INTERVAL_IN_WAITING_ROOM, INTERVAL_IN_WAITING_ROOM, TimeUnit.SECONDS);

    this.app = Javalin.create(config -> {
      config.staticFiles.add(GoWebAppConfig.WEB_APP_CONFIG.getWebRootDirectory().getName(),
          Location.CLASSPATH);
      config.staticFiles.enableWebjars();
      config.http.generateEtags = true;
      config.plugins.enableCors(cors -> cors.add(corsConfig -> corsConfig.anyHost()));
      config.accessManager(new GoAccessManager(goTables.usersTable));
    });

    GoJsonRpcService jsonRpcSrv = new GoJsonRpcService(webSocketManager, goTables);

    prepareWebSocket(app, webSocketManager);
    prepareFirebase();
    prepareJsonRpc(app, webSocketManager, jsonRpcSrv, new AuthService.Factory(goTables.usersTable,
        goTables.loginsTable, goTables.passwordsTable));


    GoAppHandlers.prepareGetHandler(app, webSocketManager, goTables);
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
    JavalinJsonRpcService srv = new JavalinJsonRpcService(GoApplication.getDefaultJacksonMapper());
    app.post("/app/json/GoJsonRpcService", ctx -> srv.handle(ctx, jsonRpcSrv));
    app.post("/app/json/AuthRpcService",
        ctx -> srv.handle(ctx, authServiceFactory.create(ctx.req())));
  }

  private static boolean prepareFirebase() {
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



  public static JacksonMapper getDefaultJacksonMapper() {
    return JacksonMapper.getIgnoreUnknownPropertiesMapper();
  }

}
