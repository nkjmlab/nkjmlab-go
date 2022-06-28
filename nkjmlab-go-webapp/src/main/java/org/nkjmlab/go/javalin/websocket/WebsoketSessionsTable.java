package org.nkjmlab.go.javalin.websocket;

import java.time.LocalDateTime;
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
import org.nkjmlab.go.javalin.model.relation.UsersTable.User;
import org.nkjmlab.go.javalin.websocket.WebsoketSessionsTable.WebSocketSession;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.annotation.OrmRecord;
import org.nkjmlab.sorm4j.sql.OrderedParameterSqlParser;
import org.nkjmlab.sorm4j.sql.ParameterizedSql;
import org.nkjmlab.sorm4j.sql.ParameterizedSqlParser;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.table_def.annotation.Index;
import org.nkjmlab.sorm4j.util.table_def.annotation.PrimaryKey;

public class WebsoketSessionsTable extends BasicH2Table<WebSocketSession> {
  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger();


  private static final String USER_ID = "user_id";
  private static final String GAME_ID = "game_id";

  private static Map<Integer, Session> sessions = new ConcurrentHashMap<>();

  public WebsoketSessionsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), WebSocketSession.class);
  }



  List<WebSocketSession> readSessionsByGameId(String gameId) {
    return readList(SELECT_STAR + FROM + getTableName() + WHERE + GAME_ID + "=?", gameId);
  }

  List<WebSocketSession> readSessionsByUserId(String userId) {
    return readList("select * " + FROM + getTableName() + WHERE + USER_ID + "=?", userId);
  }

  List<Session> getSessionsByGameId(String gameId) {
    List<Session> result =
        readSessionsByGameId(gameId).stream().map(session -> sessions.get(session.sessionId()))
            .filter(Objects::nonNull).collect(Collectors.toList());
    return result;
  }

  List<Session> getSessionsByUserId(String userId) {
    List<Session> result =
        readSessionsByUserId(userId).stream().map(session -> sessions.get(session.sessionId()))
            .filter(s -> Objects.nonNull(s)).collect(Collectors.toList());
    return result;
  }

  public List<Session> getSessionsByUserIds(List<String> userIds) {
    if (userIds.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql psql = ParameterizedSqlParser
        .parse("select * from " + getTableName() + " where " + USER_ID + " IN(<?>) ", userIds);
    return readList(psql.getSql(), psql.getParameters()).stream()
        .map(session -> sessions.get(session.sessionId())).filter(s -> Objects.nonNull(s))
        .collect(Collectors.toList());
  }



  List<Session> getAllSessions() {
    List<Session> result = selectAll().stream().map(session -> sessions.get(session.sessionId()))
        .filter(s -> Objects.nonNull(s)).collect(Collectors.toList());
    return result;
  }


  void registerSession(String gameId, String userId, Session session) {
    int sessionId = session.hashCode();
    WebSocketSession ws = new WebSocketSession(sessionId, gameId, userId, LocalDateTime.now());
    if (exists(ws)) {
      log.warn("{} already exists.", ws);
      return;
    }
    insert(ws);
    sessions.put(sessionId, session);
    log.info("WebSocket is registered={}", ws);
  }

  void updateSession(int sessionId, String gameId, String userId) {
    update(new WebSocketSession(sessionId, gameId, userId, LocalDateTime.now()));

  }

  Optional<String> removeSession(Session session) {
    for (Entry<Integer, Session> e : sessions.entrySet()) {
      if (e.getValue().equals(session)) {
        sessions.remove(e.getKey());
        WebSocketSession gs = selectByPrimaryKey(e.getKey());
        delete(gs);
        return Optional.of(gs.gameId());
      }
    }
    return Optional.empty();
  }

  public List<User> readUsers(UsersTable usersTable, String gameId) {
    Set<String> uids = readSessionsByGameId(gameId).stream().map(u -> u == null ? null : u.userId())
        .filter(Objects::nonNull).collect(Collectors.toSet());
    return usersTable.readListByUids(uids);

  }

  public List<User> readStudents(UsersTable usersTable, String gameId) {
    return readUsers(usersTable, gameId).stream().filter(u -> u.isStudent())
        .collect(Collectors.toList());
  }

  public List<String> readActiveGameIdsOrderByGameId(UsersTable usersTable) {
    ParameterizedSql psql = ParameterizedSqlParser.parse(
        SELECT + DISTINCT + GAME_ID + FROM + getTableName() + WHERE + GAME_ID + LIKE + "?",
        "%-vs-%");
    return getOrm().readList(String.class, psql).stream()
        .filter(gid -> readUsers(usersTable, gid).size() > 0).sorted().collect(Collectors.toList());
  }

  public int getWatchingUniqueStudentsNum(UsersTable usersTable, String gameId) {
    return (int) readStudents(usersTable, gameId).stream().map(uj -> uj.userId())
        .collect(Collectors.toSet()).stream().count();
  }

  public List<Session> getAdminSessions(UsersTable usersTable) {
    List<String> ids = usersTable.getAdminUserIds();
    if (ids.size() == 0) {
      return Collections.emptyList();
    }
    ParameterizedSql st = OrderedParameterSqlParser
        .parse("select * from " + getTableName() + " where " + USER_ID + " IN (<?>)", ids);
    return readList(st.getSql(), st.getParameters()).stream()
        .map(ws -> sessions.get(ws.sessionId())).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }


  @OrmRecord
  public static record WebSocketSession(@PrimaryKey int sessionId, String userId,
      @Index String gameId, LocalDateTime createdAt) {

  }

}
