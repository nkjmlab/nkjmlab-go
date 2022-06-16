package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.util.sql.SelectSql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.context.SormContext;
import org.nkjmlab.sorm4j.util.h2.BasicH2Table;
import org.nkjmlab.sorm4j.util.logger.LoggerContext;
import org.nkjmlab.sorm4j.util.table_def.TableDefinition;

public class LoginsTable extends BasicH2Table<Login> {

  public static final String TABLE_NAME = "LOGINS";

  private static final String ID = "id";
  private static final String USER_ID = "user_id";
  private static final String SEAT_ID = "seat_id";
  private static final String USER_NAME = "user_name";
  private static final String LOGGED_IN_AT = "logged_in_at";
  private static final String REMOTE_ADDR = "remote_addr";

  private Sorm loggerableSorm;

  public LoginsTable(DataSource dataSource) {
    super(Sorm.create(dataSource), Login.class,
        TableDefinition.builder(TABLE_NAME)
            .addColumnDefinition(ID, BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
            .addColumnDefinition(USER_ID, VARCHAR).addColumnDefinition(SEAT_ID, VARCHAR)
            .addColumnDefinition(USER_NAME, VARCHAR).addColumnDefinition(LOGGED_IN_AT, TIMESTAMP)
            .addColumnDefinition(REMOTE_ADDR, VARCHAR).addIndexDefinition(USER_ID).build());
    this.loggerableSorm = Sorm.create(dataSource, SormContext.builder()
        .setLoggerContext(LoggerContext.builder().enableAll().build()).build());
  }



  private List<Login> readAllLastLoginsOrderByUserId() {
    return readList(selectStarFrom(TABLE_NAME)
        + where("ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)") + orderBy(USER_ID));
  }

  public List<Login> readOrderedActiveStudentLogins(UsersTable usersTable) {
    LocalDate nowDate = LocalDate.now();
    return readAllLastLoginsOrderByUserId().stream()
        .filter(l -> Optional.ofNullable(usersTable.readByPrimaryKey(l.getUserId()))
            .map(u -> u.isStudent()).orElse(false)
            && l.getLoggedInAt().toLocalDate().equals(nowDate))
        .collect(Collectors.toList());
  }

  public String getNextLoginUserId(UsersTable usersTable, String userId) {
    // List<String> users = readOrderedActiveStudentLogins(usersTable).stream().map(l ->
    // l.getUserId())
    // .collect(Collectors.toList());


    List<String> users = usersTable.getStudentUserIds();
    return getNext(users, userId);
  }

  public String getPrevLoginUserId(UsersTable usersTable, String userId) {
    // List<String> users = readOrderedActiveStudentLogins(usersTable).stream().map(l ->
    // l.getUserId())
    // .collect(Collectors.toList());
    List<String> users = usersTable.getStudentUserIds();
    Collections.reverse(users);
    return getNext(users, userId);
  }

  private static String getNext(List<String> userIds, String userId) {
    for (int i = 0; i < userIds.size() - 1; i++) {
      if (userIds.get(i).equals(userId)) {
        return userIds.get((i + 1) % userIds.size());
      }
    }
    return userIds.size() == 0 ? "" : userIds.get(0);
  }

  public void login(User u, String remoteAddr) {
    insert(new Login(u, LocalDateTime.now(), remoteAddr));
  }



  public Optional<Login> readLastLogin(String userId) {

    Login ret = loggerableSorm.applyHandler(conn -> {
      Login l = conn.readFirst(Login.class,
          "SELECT * FROM LOGINS WHERE USER_ID=? ORDER BY LOGGED_IN_AT DESC LIMIT 1", userId);
      return l;
    });
    return Optional.ofNullable(ret);

  }

  public boolean isAttendance(String userId) {
    return readLastLogin(userId).map(l -> l.getLoggedInAt().toLocalDate().equals(LocalDate.now()))
        .orElse(false);
  }



}
