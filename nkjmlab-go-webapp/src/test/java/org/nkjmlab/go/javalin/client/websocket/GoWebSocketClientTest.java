package org.nkjmlab.go.javalin.client.websocket;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.Test;
import org.nkjmlab.sorm4j.util.function.exception.Try;

class GoWebSocketClientTest {

  private static final int num = 64;

  @Test
  void testLocal() {
    String url = "ws://localhost:12345/websocket/play";
    for (int i = 0; i < num; i++) {
      int stdId = 5519000 + i;
      new GoWebSocketClient(url).start(stdId);
    }
  }

  public static class GoWebSocketClient {
    private static final org.apache.logging.log4j.Logger log =
        org.apache.logging.log4j.LogManager.getLogger();

    private final WebSocketClient client = new WebSocketClient();
    private final String uri;

    public GoWebSocketClient(String uri) {
      this.uri = uri;
    }

    public void start(int stdId) {
      try {
        client.start();
        URI toUri = new URI(uri + "?userId=" + stdId + "&gameId=" + stdId);
        client.connect(new GoWebSocket(), toUri, new ClientUpgradeRequest()).get();

      } catch (Exception t) {
        throw Try.rethrow(t);
      } finally {
        try {
          client.stop();
        } catch (Exception e) {
          log.error(e, e);
        }
      }
    }

    @WebSocket
    public static class GoWebSocket {

      private static final org.apache.logging.log4j.Logger log =
          org.apache.logging.log4j.LogManager.getLogger();

      private static AtomicInteger onConnectCounter = new AtomicInteger(0);
      private static AtomicInteger onMessageCounter = new AtomicInteger(0);

      @OnWebSocketConnect
      public void onConnect(Session session) {
        log.info("onConnectCounter={}", onConnectCounter.getAndIncrement());
      }

      @OnWebSocketMessage
      public void onMessage(Session session, String message) {
        log.info("onMessageCounter={}", onMessageCounter.getAndIncrement());
        log.debug("onMessage={}", message);
      }

      @OnWebSocketClose
      public void onClose(Session session, int statusCode, String reason) {
        try {
        } catch (Throwable e) {
          log.error(e, e);
          throw e;
        }
      }

      @OnWebSocketError
      public void onError(Session session, Throwable cause) {
        try {
          log.info("onError");
          log.error(cause, cause);
        } catch (Throwable e) {
          log.error(e, e);
          throw e;
        }
      }
    }
  }
}
