package org.nkjmlab.go.javalin.client.websocket;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.nkjmlab.util.lang.ThreadUtils;

public class GoWebSocketClient {
  private static org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger();
  private static final int num = 64;

  private final String uri;
  private final WebSocketClient client = new WebSocketClient();
  private final GoWebSocket webSocket = new GoWebSocket();

  public static void main(String[] args) {
    String url = "ws://localhost:4567/websocket/play";
    new GoWebSocketClient(url).start();
  }

  public GoWebSocketClient(String uri) {
    this.uri = uri;
  }

  private void start() {
    try {
      client.start();

      for (int i = 0; i < num; i++) {
        int stdId = 5519000 + i;
        URI toUri = new URI(uri + "?userId=" + stdId + "&gameId=" + stdId);
        client.connect(webSocket, toUri, new ClientUpgradeRequest());
      }

    } catch (Throwable t) {
      log.error(t, t);
    } finally {
      try {
        ThreadUtils.sleep(20, TimeUnit.SECONDS);
        client.stop();
      } catch (Exception e) {
        log.error(e, e);
      }
    }
  }
}
