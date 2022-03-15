package org.nkjmlab.go.javalin.client.websocket;

class GoWebSocketClientTest {

  void test() {
    String url = "ws://localhost:4567/websocket/play";
    new GoWebSocketClient(url).start();
  }

}
