package org.nkjmlab.go.javalin.client.websocket;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class GoWebSocketClient {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();
  private static final int num = 64;

  private final String uri;
  private final WebSocketClient client = new WebSocketClient();
  private final GoWebSocket webSocket = new GoWebSocket();

  public GoWebSocketClient(String uri) {
    this.uri = uri;
  }

  public void start() {
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
        TimeUnit.SECONDS.sleep(20);
        client.stop();
      } catch (Exception e) {
        log.error(e, e);
      }
    }
  }
}
