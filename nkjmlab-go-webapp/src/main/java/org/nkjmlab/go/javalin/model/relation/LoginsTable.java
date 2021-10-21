package org.nkjmlab.go.javalin.model.relation;

import static org.nkjmlab.sorm4j.sql.SelectSql.*;
import static org.nkjmlab.sorm4j.sql.schema.TableSchemaKeyword.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.nkjmlab.go.javalin.model.row.Login;
import org.nkjmlab.go.javalin.model.row.User;
import org.nkjmlab.sorm4j.Sorm;
import org.nkjmlab.sorm4j.sql.schema.TableSchema;

public class LoginsTable {

  public static final String TABLE_NAME = "LOGINS";

  private static final String ID = "id";
  private static final String USER_ID = "user_id";
  private static final String SEAT_ID = "seat_id";
  private static final String USER_NAME = "user_name";
  private static final String LOGGED_IN_AT = "logged_in_at";
  private static final String REMOTE_ADDR = "remote_addr";

  private Sorm sorm;
  private TableSchema schema;

  public LoginsTable(DataSource dataSource) {
    this.sorm = Sorm.create(dataSource);
    this.schema = TableSchema.builder(TABLE_NAME)
        .addColumnDefinition(ID, BIGINT, AUTO_INCREMENT, PRIMARY_KEY)
        .addColumnDefinition(USER_ID, VARCHAR).addColumnDefinition(SEAT_ID, VARCHAR)
        .addColumnDefinition(USER_NAME, VARCHAR).addColumnDefinition(LOGGED_IN_AT, TIMESTAMP)
        .addColumnDefinition(REMOTE_ADDR, VARCHAR).addIndexColumn(USER_ID).build();
    schema.createTableAndIndexesIfNotExists(sorm);
  }



  private List<Login> readAllLastLoginsOrderByUserId() {
    return sorm.readList(Login.class, selectFrom(TABLE_NAME)
        + where("ID IN (SELECT MAX(ID) FROM LOGINS GROUP BY USER_ID)") + orderBy(USER_ID));
  }

  public List<Login> readOrderedActiveStudentLogins(UsersTable usersTable) {
    Date now = new Date();
    return readAllLastLoginsOrderByUserId().stream()
        .filter(l -> Optional.ofNullable(usersTable.readByPrimaryKey(l.getUserId()))
            .map(u -> u.isStudent()).orElse(false) && DateUtils.isSameDay(l.getLoggedInAt(), now))
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
    insert(new Login(u, new Timestamp(new Date().getTime()), remoteAddr));
  }


  public void insert(Login login) {
    sorm.insert(login);
  }

  public Optional<Login> readLastLogin(String userId) {
    Login ret = sorm.applyWithLogging(conn -> conn.readFirst(Login.class,
        "SELECT * FROM LOGINS WHERE USER_ID=? ORDER BY LOGGED_IN_AT DESC LIMIT 1", userId));
    return Optional.ofNullable(ret);

  }

  public boolean isAttendance(String userId) {
    return readLastLogin(userId)
        .map(l -> l.getLoggedInAt().toLocalDateTime().toLocalDate().equals(LocalDate.now()))
        .orElse(false);
  }



}
