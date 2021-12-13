package org.nkjmlab.go.javalin.client.websocket;

import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class GoWebSocket {

  private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger();

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
