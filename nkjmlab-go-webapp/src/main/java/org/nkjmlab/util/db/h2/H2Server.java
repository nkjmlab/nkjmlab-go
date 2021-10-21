package org.nkjmlab.util.db.h2;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.h2.server.web.WebServer;
import org.h2.tools.Server;
import org.nkjmlab.util.lang.Try;

public class H2Server {
  private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

  private static final String DEFALUT_H2_CLASS_PATH = getClassPathOf("^h2-.*.jar$");
  public static final int DEFAULT_TCP_PORT = 9092;
  public static final String DEFAULT_TCP_PASSWORD = "DEFAULT_PASSWORD_9092";

  private static final long DEFAULT_START_WAIT_TIME = 4;
  private static final TimeUnit DEFAULT_START_WAIT_TIME_UNIT = TimeUnit.SECONDS;
  private static final long DEFAULT_SHUTDOWN_WAIT_TIME = 2000;

  private String classpath;
  private int tcpPort;
  private String tcpPassword;
  private String[] serverOptions;


  public H2Server() {
    this.classpath = DEFALUT_H2_CLASS_PATH;
    this.tcpPort = DEFAULT_TCP_PORT;
    this.tcpPassword = DEFAULT_TCP_PASSWORD;
    this.serverOptions = new String[0];
  }

  public H2Server(String classpath, int tcpPort, String tcpPassword, String... serverOptions) {
    this.classpath = classpath;
    this.tcpPort = tcpPort;
    this.tcpPassword = tcpPassword;
    this.serverOptions = serverOptions;
  }



  public static void main(String[] args) {
    shutdown();
    startAndWait();
  }


  public void startAndWaitFor(long waitTime, TimeUnit unit) {
    startAndWait(classpath, tcpPort, tcpPassword, waitTime, unit, serverOptions);
  }

  public void shutdownAndWaitFor(long waitMillis) {
    shutdown(tcpPort, tcpPassword, waitMillis);
  }


  public static void startAndWait() {
    startAndWait(DEFAULT_TCP_PORT, DEFAULT_TCP_PASSWORD);
  }

  public static void shutdown() {
    shutdown(DEFAULT_TCP_PORT, DEFAULT_TCP_PASSWORD, DEFAULT_SHUTDOWN_WAIT_TIME);
  }

  public static void startAndWait(int tcpPort, String tcpPassword, String... options) {
    startAndWait(DEFALUT_H2_CLASS_PATH, tcpPort, tcpPassword, DEFAULT_START_WAIT_TIME,
        DEFAULT_START_WAIT_TIME_UNIT, options);
  }


  /**
   *
   * @param tcpPassword is a password of tcpPassword Of H2 Server. not password of DB admin.
   */
  public static void shutdown(int tcpPort, String tcpPassword, long waitMillis) {
    if (!isActive(tcpPort)) {
      log.info("H2 server is not active.");
      return;
    }
    try {
      log.info("Try to start shutdown h2 server...");
      Server.shutdownTcpServer("tcp://localhost:" + tcpPort, tcpPassword, false, false);
      Thread.sleep(waitMillis);
    } catch (SQLException | InterruptedException e) {
      log.error(e.getMessage());
    }
    if (isActive(tcpPort)) {
      log.warn("H2 server is still active.");
    } else {
      log.info("H2 server is stopped.");
    }

  }

  public static String getClassPathOf(String regex) {
    for (String cp : System.getProperty("java.class.path").split(File.pathSeparator)) {
      if (new File(cp).getName().matches(regex)) {
        return cp;
      }
    }
    throw new RuntimeException(regex + " not found");
  }

  public static void startAndWait(String h2ClassPath, int tcpPort, String tcpPassword, long timeout,
      TimeUnit unit, String... options) {
    if (isActive(tcpPort)) {
      log.info("H2 server has been already activated.");
      return;
    }
    List<String> args = new ArrayList<>();
    args.add("java");
    args.addAll(List.of("-cp", h2ClassPath, "org.h2.tools.Server"));

    List<String> _opts = new ArrayList<>(List.of("-web", "-tcp", "-ifNotExists"));
    _opts.addAll(List.of("-tcpPort", tcpPort + ""));
    _opts.addAll(List.of("-tcpPassword", tcpPassword));
    _opts.addAll(Arrays.asList(options));
    args.addAll(_opts);


    try {
      ProcessBuilder pb = new ProcessBuilder(args.toArray(String[]::new));
      pb.redirectErrorStream(true);
      Process process = pb.start();
      log.info("Try to start H2 server and wait [{} {}], command= {}", timeout, unit, args);
      process.waitFor(timeout, unit);

      if (isActive(tcpPort)) {
        log.info("H2 server is activated.");
      } else {
        log.error("Fail to start h2 server.");
      }
    } catch (IOException | InterruptedException e) {
      log.error(e.getMessage());
    }

  }

  private static boolean isActive(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      return false;
    } catch (IOException e) {
      return true;
    }
  }



  public static void openBrowser(Connection conn, boolean keepAlive) {
    try {
      Server server = Server.createWebServer(keepAlive ? new String[] {"-webPort", "0"}
          : new String[] {"-webPort", "0", "-webDaemon"});
      server.start();
      log.info("H2 Temporal WebServer is start at {}", server.getURL());

      WebServer webServer = (WebServer) server.getService();
      webServer.addSession(conn);
      String url = webServer.addSession(conn);
      Server.openBrowser(url);
      log.info("Database open on browser = {}", url);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void openBrowser(DataSource dataSource, boolean keepAlive) {
    Try.runOrThrow(() -> openBrowser(dataSource.getConnection(), keepAlive), Try::rethrow);
  }

}
