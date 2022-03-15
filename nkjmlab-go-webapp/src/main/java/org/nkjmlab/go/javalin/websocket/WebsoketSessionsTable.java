package org.nkjmlab.go.javalin.websocket;

import static org.nkjmlab.sorm4j.util.sql.SqlKeyword.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.eclipse.jetty.websocket.api.Session;
import org.nkjmlab.go.javalin.model.relation.UsersTable;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;

public class WebsoketSessionsTable {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();

  private Sorm sorm;

  private TableDefinition schema;

  public static final String TABLE_NAME = "WEBSOCKET_SESSIONS";

  private static final String SESSION_ID = "session_id";
  private static final String USER_ID = "user_id";
  private static final String GAME_ID = "game_id";
  private static final String CREATED_AT = "created_at";
  private static final String GLOBAL_MESSAGE_COUNT = "global_message_count";

  private static Map<Integer, Session> sessions = new ConcurrentHashMap<>();

  public WebsoketSessionsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableDefinition.builder(TABLE_NAME).addColumnDefinition(SESSION_ID, INT, PRIMARY_KEY)
        .addColumnDefinition(USER_ID, VARCHAR).addColumnDefinition(GAME_ID, VARCHAR)
        .addColumnDefinition(CREATED_AT, TIMESTAMP).addColumnDefinition(GLOBAL_MESSAGE_COUNT, INT)
        .addIndexDefinition(GAME_ID).build();
    schema.createTableIfNotExists(sorm).createIndexesIfNotExists(sorm);
  }



  List<WebSocketSession> readSessionsByGameId(String gameId) {
    return sorm.readList(WebSocketSession.class,
        SELECT_STAR + FROM + TABLE_NAME + WHERE + GAME_ID + "=?", gameId);
  }

  List<WebSocketSession> readSessionsByUserId(String userId) {
    return sorm.readList(WebSocketSession.class,
        "select * " + FROM + TABLE_NAME + WHERE + USER_ID + "=?", userId);
  }

  List<Session> getSessionsByGameId(String gameId) {
    List<Session> result =
        readSessionsByGameId(gameId).stream().map(session -> sessions.get(session.getSessionId()))
            .filter(Objects::nonNull).collect(Collectors.toList());
    return result;
  }

  List<Session> getSessionsByUserId(String userId) {
    List<Session> result =
        readSessionsByUserId(userId).stream().map(session -> sessions.get(session.getSessionId()))
            .filter(s -> Objects.nonNull(s)).collect(Collectors.toList());
    return result;
  }

  public List<Session> getSessionsByUserIds(List<String> userIds) {
    if (userIds.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql psql = ParameterizedSqlParser
        .parse("select * from " + TABLE_NAME + " where " + USER_ID + " IN(<?>) ", userIds);
    return sorm.readList(WebSocketSession.class, psql.getSql(), psql.getParameters()).stream()
        .map(session -> sessions.get(session.getSessionId())).filter(s -> Objects.nonNull(s))
        .collect(Collectors.toList());
  }



  List<Session> getAllSessions() {
    List<Session> result = sorm.selectAll(WebSocketSession.class).stream()
        .map(session -> sessions.get(session.getSessionId())).filter(s -> Objects.nonNull(s))
        .collect(Collectors.toList());
    return result;
  }


  void registerSession(String gameId, String userId, Session session) {
    int sessionId = session.hashCode();
    WebSocketSession ws = new WebSocketSession(sessionId, gameId, userId);
    if (sorm.exists(ws)) {
      log.warn("{} already exists.", ws);
      return;
    }
    sorm.insert(ws);
    sessions.put(sessionId, session);
    log.info("WebSocket is registered={}", ws);
  }

  void updateSession(int sessionId, String gameId, String userId) {
    sorm.update(new WebSocketSession(sessionId, gameId, userId));

  }

  Optional<String> removeSession(Session session) {
    for (Entry<Integer, Session> e : sessions.entrySet()) {
      if (e.getValue().equals(session)) {
        sessions.remove(e.getKey());
        WebSocketSession gs = sorm.selectByPrimaryKey(WebSocketSession.class, e.getKey());
        sorm.delete(gs);
        return Optional.of(gs.getGameId());
      }
    }
    return Optional.empty();
  }

  public List<User> readUsers(UsersTable usersTable, String gameId) {
    Set<String> uids =
        readSessionsByGameId(gameId).stream().map(u -> u == null ? null : u.getUserId())
            .filter(Objects::nonNull).collect(Collectors.toSet());
    return usersTable.readListByUids(uids);

  }

  public List<User> readStudents(UsersTable usersTable, String gameId) {
    return readUsers(usersTable, gameId).stream().filter(u -> u.isStudent())
        .collect(Collectors.toList());
  }

  public List<String> readActiveGameIdsOrderByGameId(UsersTable usersTable) {
    ParameterizedSql psql = ParameterizedSqlParser.parse(
        SELECT + DISTINCT + GAME_ID + FROM + TABLE_NAME + WHERE + GAME_ID + LIKE + "?", "%-vs-%");
    return sorm.readList(String.class, psql).stream()
        .filter(gid -> readUsers(usersTable, gid).size() > 0).sorted().collect(Collectors.toList());
  }

  public int getWatchingUniqueStudentsNum(UsersTable usersTable, String gameId) {
    return (int) readStudents(usersTable, gameId).stream().map(uj -> uj.getUserId())
        .collect(Collectors.toSet()).stream().count();
  }

  public List<Session> getAdminSessions(UsersTable usersTable) {
    List<String> ids = usersTable.getAdminUserIds();
    if (ids.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql st = OrderedParameterSqlParser
        .parse("select * from " + TABLE_NAME + " where " + USER_ID + " IN (<?>)", ids);
    return sorm.readList(WebSocketSession.class, st.getSql(), st.getParameters()).stream()
        .map(ws -> sessions.get(ws.getSessionId())).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }


}
